package cl.duoc.pagoflex.recomendaciones.service;

import cl.duoc.pagoflex.recomendaciones.client.AuthGateway;
import cl.duoc.pagoflex.recomendaciones.dto.CambiarEstadoRequest;
import cl.duoc.pagoflex.recomendaciones.dto.CambioDto;
import cl.duoc.pagoflex.recomendaciones.dto.ContactoDto;
import cl.duoc.pagoflex.recomendaciones.dto.NuevaRecomendacionRequest;
import cl.duoc.pagoflex.recomendaciones.dto.RecomendacionDto;
import cl.duoc.pagoflex.recomendaciones.dto.ResumenDto;
import cl.duoc.pagoflex.recomendaciones.dto.UsuarioRemoto;
import cl.duoc.pagoflex.recomendaciones.exception.NegocioException;
import cl.duoc.pagoflex.recomendaciones.model.Catalogo;
import cl.duoc.pagoflex.recomendaciones.model.ContactoRecomendacion;
import cl.duoc.pagoflex.recomendaciones.model.HistorialRecomendacion;
import cl.duoc.pagoflex.recomendaciones.model.Recomendacion;
import cl.duoc.pagoflex.recomendaciones.repository.CatalogoRepository;
import cl.duoc.pagoflex.recomendaciones.repository.ContactoRepository;
import cl.duoc.pagoflex.recomendaciones.repository.HistorialRepository;
import cl.duoc.pagoflex.recomendaciones.repository.RecomendacionRepository;
import cl.duoc.pagoflex.recomendaciones.util.RutUtil;
import cl.duoc.pagoflex.recomendaciones.validacion.ValidacionesRecomendacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecomendacionService {

    private final RecomendacionRepository recomendaciones;
    private final ContactoRepository contactos;
    private final HistorialRepository historial;
    private final CatalogoRepository catalogos;
    private final AuthGateway auth;
    private final Clock reloj;

    public RecomendacionService(RecomendacionRepository recomendaciones, ContactoRepository contactos,
                                HistorialRepository historial, CatalogoRepository catalogos, AuthGateway auth,
                                Clock reloj) {
        this.recomendaciones = recomendaciones;
        this.contactos = contactos;
        this.historial = historial;
        this.catalogos = catalogos;
        this.auth = auth;
        this.reloj = reloj;
    }

    private LocalDateTime ahora(LocalDateTime pedido) {
        return pedido != null ? pedido : LocalDateTime.now(reloj);
    }

    private String nombreEstado(Integer id) {
        return catalogos.findByTipoAndCodigo(ReglasRecomendacion.TIPO_ESTADO, id).map(Catalogo::getNombre).orElse("");
    }

    private List<RecomendacionDto> armar(List<Recomendacion> lista) {
        List<Integer> ids = lista.stream().map(Recomendacion::getIdRecomendacion).toList();
        Map<Integer, ContactoRecomendacion> contactoPorId = contactos.findByIdRecomendacionIn(ids).stream()
                .collect(Collectors.toMap(ContactoRecomendacion::getIdRecomendacion, Function.identity()));
        Map<Integer, List<HistorialRecomendacion>> cambiosPorId = historial.findByIdRecomendacionInOrderByFecha(ids)
                .stream().collect(Collectors.groupingBy(HistorialRecomendacion::getIdRecomendacion));
        return lista.stream().map(r -> {
            ContactoRecomendacion c = contactoPorId.get(r.getIdRecomendacion());
            ContactoDto contacto = c != null && Boolean.TRUE.equals(c.getAutorizaContacto())
                    ? new ContactoDto(c.getNombre(), c.getCorreo(), c.getTelefono())
                    : null;
            List<CambioDto> cambios = cambiosPorId.getOrDefault(r.getIdRecomendacion(), List.of()).stream()
                    .map(h -> new CambioDto(h.getIdEstadoRecomendacion(), h.getIdAgente(), h.getFecha(), h.getMotivo()))
                    .toList();
            return new RecomendacionDto(r.getIdRecomendacion(), r.getNumero(), r.getIdUsuarioFinal(),
                    r.getNombreOrganizacion(), r.getRutOrganizacion(), r.getIdRubro(), r.getIdComuna(), r.getFecha(),
                    r.getIdEstadoRecomendacion(), contacto, cambios);
        }).toList();
    }

    public List<RecomendacionDto> listar(Integer idUsuario) {
        List<Recomendacion> lista = idUsuario == null
                ? recomendaciones.findAllByOrderByFechaDesc()
                : recomendaciones.findByIdUsuarioFinalOrderByFechaDesc(idUsuario);
        return armar(lista);
    }

    public RecomendacionDto obtener(Integer id) {
        return armar(List.of(buscar(id))).get(0);
    }

    private Recomendacion buscar(Integer id) {
        return recomendaciones.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No encontramos la recomendación."));
    }

    public ResumenDto resumen(Integer idUsuario, LocalDate hoy) {
        LocalDate dia = hoy != null ? hoy : LocalDate.now(reloj);
        YearMonth mes = YearMonth.from(dia);
        long enviadas = recomendaciones.countByIdUsuarioFinalAndFechaGreaterThanEqualAndFechaLessThan(
                idUsuario, mes.atDay(1).atStartOfDay(), mes.plusMonths(1).atDay(1).atStartOfDay());
        long seUnieron = recomendaciones.countByIdUsuarioFinalAndIdEstadoRecomendacion(
                idUsuario, ReglasRecomendacion.SE_UNIO);
        return new ResumenDto(enviadas, seUnieron);
    }

    @Transactional
    public RecomendacionDto crear(NuevaRecomendacionRequest p) {
        UsuarioRemoto persona = p.idUsuario() == null ? null : auth.usuario(p.idUsuario()).orElse(null);
        if (persona == null || persona.idRol() != ReglasRecomendacion.ROL_USUARIO_FINAL
                || !Boolean.TRUE.equals(persona.activo())) {
            throw new NegocioException(HttpStatus.FORBIDDEN,
                    "Solo una persona con cuenta de pagos puede recomendar organizaciones.");
        }
        String contactoNombre = p.contactoNombre() == null ? "" : p.contactoNombre();
        String contactoCorreo = p.contactoCorreo() == null ? "" : p.contactoCorreo();
        String contactoTelefono = p.contactoTelefono() == null ? "" : p.contactoTelefono();
        String rut = p.rutOrganizacion() == null ? "" : p.rutOrganizacion();
        String error = ValidacionesRecomendacion.primerError(p.organizacion(), p.idRubro(), p.idComuna(), rut,
                contactoNombre, contactoCorreo, contactoTelefono, Boolean.TRUE.equals(p.autorizaContacto()));
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        if (!catalogos.existsByTipoAndCodigo(ReglasRecomendacion.TIPO_RUBRO, p.idRubro())
                || !auth.existeComuna(p.idComuna())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige el rubro y la comuna de la organización.");
        }
        String nombre = ReglasRecomendacion.normalizarNombre(p.organizacion());
        recomendaciones.findByIdUsuarioFinalAndNombreOrganizacionIgnoreCase(p.idUsuario(), nombre)
                .ifPresent(previa -> {
                    throw new NegocioException(HttpStatus.CONFLICT, "Ya recomendaste a «" + previa.getNombreOrganizacion()
                            + "» (" + previa.getNumero() + "). Su estado actual es: "
                            + nombreEstado(previa.getIdEstadoRecomendacion()) + ".");
                });
        LocalDateTime fecha = ahora(p.fechaHora());
        ResumenDto resumen = resumen(p.idUsuario(), fecha.toLocalDate());
        if (resumen.enviadasEsteMes() >= ReglasRecomendacion.MAX_POR_MES) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya enviaste " + ReglasRecomendacion.MAX_POR_MES
                    + " recomendaciones este mes. El próximo mes podrás enviar más.");
        }
        Recomendacion nueva = new Recomendacion();
        nueva.setNumero(ReglasRecomendacion.siguienteNumero(
                recomendaciones.findTopByOrderByNumeroDesc().map(Recomendacion::getNumero).orElse(null)));
        nueva.setIdUsuarioFinal(p.idUsuario());
        nueva.setNombreOrganizacion(nombre);
        nueva.setRutOrganizacion(rut.isBlank() ? null : RutUtil.formatear(rut));
        nueva.setIdRubro(p.idRubro());
        nueva.setIdComuna(p.idComuna());
        nueva.setFecha(fecha);
        nueva.setIdEstadoRecomendacion(ReglasRecomendacion.RECIBIDA);
        Recomendacion guardada = recomendaciones.save(nueva);
        if (!contactoNombre.isBlank()) {
            contactos.save(new ContactoRecomendacion(guardada.getIdRecomendacion(), contactoNombre.trim(),
                    contactoCorreo.isBlank() ? null : contactoCorreo.trim(),
                    contactoTelefono.isBlank() ? null : contactoTelefono.trim(),
                    Boolean.TRUE.equals(p.autorizaContacto())));
        }
        return armar(List.of(guardada)).get(0);
    }

    @Transactional
    public RecomendacionDto cambiarEstado(Integer id, CambiarEstadoRequest p) {
        UsuarioRemoto agente = p.idAgente() == null ? null : auth.usuario(p.idAgente()).orElse(null);
        if (agente == null || agente.idRol() != ReglasRecomendacion.ROL_AGENTE
                || !Boolean.TRUE.equals(agente.activo())) {
            throw new NegocioException(HttpStatus.FORBIDDEN,
                    "Solo un agente PagoFlex activo puede cambiar el estado de una recomendación.");
        }
        Recomendacion r = buscar(id);
        if (p.idEstado() == null || !catalogos.existsByTipoAndCodigo(ReglasRecomendacion.TIPO_ESTADO, p.idEstado())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige el nuevo estado.");
        }
        int actual = r.getIdEstadoRecomendacion();
        if (ReglasRecomendacion.esFinal(actual)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Esta recomendación ya está cerrada como «"
                    + nombreEstado(actual) + "» y no se puede cambiar.");
        }
        if (!ReglasRecomendacion.puedeCambiar(actual, p.idEstado())) {
            String permitidos = ReglasRecomendacion.estadosSiguientes(actual).stream()
                    .map(e -> "«" + nombreEstado(e) + "»").collect(Collectors.joining(" o "));
            throw new NegocioException(HttpStatus.CONFLICT, "No puedes saltarte etapas. Desde «"
                    + nombreEstado(actual) + "» el siguiente paso es " + permitidos + ".");
        }
        String texto = p.motivo() == null ? "" : p.motivo().trim();
        boolean descarte = p.idEstado() == ReglasRecomendacion.DESCARTADA;
        if (descarte && texto.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Escribe el motivo para descartar la recomendación.");
        }
        if (descarte && texto.length() > ReglasRecomendacion.MAX_MOTIVO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El motivo puede tener hasta " + ReglasRecomendacion.MAX_MOTIVO + " letras.");
        }
        r.setIdEstadoRecomendacion(p.idEstado());
        recomendaciones.save(r);
        historial.save(new HistorialRecomendacion(null, id, p.idEstado(), p.idAgente(), ahora(p.fechaHora()),
                descarte ? texto : null));
        return armar(List.of(r)).get(0);
    }
}
