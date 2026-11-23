package cl.duoc.pagoflex.pagos.service;

import cl.duoc.pagoflex.pagos.client.AuthGateway;
import cl.duoc.pagoflex.pagos.dto.AnularRequest;
import cl.duoc.pagoflex.pagos.dto.PagarRequest;
import cl.duoc.pagoflex.pagos.dto.PersonaRemotaRequest;
import cl.duoc.pagoflex.pagos.dto.RegistrarCompromisoRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.dto.VencimientosRequest;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Anulacion;
import cl.duoc.pagoflex.pagos.model.Compromiso;
import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import cl.duoc.pagoflex.pagos.model.Pago;
import cl.duoc.pagoflex.pagos.repository.AnulacionRepository;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import cl.duoc.pagoflex.pagos.repository.PagoRepository;
import cl.duoc.pagoflex.pagos.util.RutUtil;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompromisoService {

    public static final int MAX_CONCEPTO = 120;
    public static final int MAX_COMENTARIO_ANULACION = 255;

    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ]{8,16}$");

    private final CompromisoRepository compromisos;
    private final PagoRepository pagos;
    private final AnulacionRepository anulaciones;
    private final EmpresaRepository empresas;
    private final CatalogoRepository catalogos;
    private final AuthGateway auth;
    private final Clock reloj;

    public CompromisoService(CompromisoRepository compromisos, PagoRepository pagos, AnulacionRepository anulaciones,
                             EmpresaRepository empresas, CatalogoRepository catalogos, AuthGateway auth, Clock reloj) {
        this.compromisos = compromisos;
        this.pagos = pagos;
        this.anulaciones = anulaciones;
        this.empresas = empresas;
        this.catalogos = catalogos;
        this.auth = auth;
        this.reloj = reloj;
    }

    private LocalDateTime ahora(LocalDateTime pedido) {
        return pedido != null ? pedido : LocalDateTime.now(reloj);
    }

    public List<Compromiso> listar(Integer idUsuario, Integer idEmpresa, Collection<Integer> ids) {
        if (idUsuario != null) {
            return compromisos.findByIdUsuarioFinalOrderByFechaVencimiento(idUsuario);
        }
        if (idEmpresa != null) {
            return compromisos.findByIdEmpresaOrderByFechaVencimiento(idEmpresa);
        }
        if (ids != null) {
            return compromisos.findAllById(ids);
        }
        return compromisos.findAll();
    }

    public Compromiso obtener(Integer id) {
        return compromisos.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No encontramos este compromiso."));
    }

    public List<Pago> pagosDe(Integer idUsuario, Integer idEmpresa, Integer idCompromiso) {
        if (idCompromiso != null) {
            return pagos.findByIdCompromisoIn(List.of(idCompromiso));
        }
        List<Integer> ids = listar(idUsuario, idEmpresa, null).stream().map(Compromiso::getIdCompromiso).toList();
        return pagos.findByIdCompromisoIn(ids);
    }

    public List<Anulacion> anulacionesDe(Integer idUsuario, Integer idEmpresa, Integer idCompromiso) {
        if (idCompromiso != null) {
            return anulaciones.findByIdCompromisoIn(List.of(idCompromiso));
        }
        List<Integer> ids = listar(idUsuario, idEmpresa, null).stream().map(Compromiso::getIdCompromiso).toList();
        return anulaciones.findByIdCompromisoIn(ids);
    }

    @Transactional
    public Pago pagar(Integer idCompromiso, PagarRequest peticion) {
        UsuarioRemoto persona = peticion.idUsuario() == null ? null : auth.usuario(peticion.idUsuario()).orElse(null);
        if (persona == null || persona.idRol() != ReglasPagos.ROL_USUARIO_FINAL) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo una persona con cuenta de pagos puede pagar.");
        }
        Compromiso c = obtener(idCompromiso);
        if (!c.getIdUsuarioFinal().equals(peticion.idUsuario())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Este compromiso no corresponde a tu cuenta.");
        }
        String motivo = motivoNoPagable(c.getIdEstadoCompromiso());
        if (motivo != null) {
            throw new NegocioException(HttpStatus.CONFLICT, motivo);
        }
        if (pagos.existsByIdCompromiso(idCompromiso)) {
            throw new NegocioException(HttpStatus.CONFLICT, "Este compromiso ya tiene un pago registrado.");
        }
        if (peticion.idMedioPago() == null
                || !catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MEDIO_PAGO, peticion.idMedioPago())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige un medio de pago para continuar.");
        }
        int canal = peticion.idCanal() != null ? peticion.idCanal() : ReglasPagos.CANAL_APP;
        if (!catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_CANAL, canal)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El canal de pago no existe.");
        }
        LocalDateTime fecha = ahora(peticion.fechaHora());
        Pago pago = new Pago();
        pago.setNumeroComprobante(siguienteComprobante());
        pago.setIdCompromiso(idCompromiso);
        pago.setFechaHora(fecha);
        pago.setMontoPagado(c.getMonto() + c.getRecargoAplicado());
        pago.setIdCanal(canal);
        pago.setIdMedioPago(peticion.idMedioPago());
        Pago guardado = pagos.save(pago);
        c.setIdEstadoCompromiso(ReglasPagos.PAGADO);
        c.setFechaCambioEstado(fecha);
        compromisos.save(c);
        return guardado;
    }

    private String motivoNoPagable(int idEstado) {
        if (idEstado == ReglasPagos.PAGADO) {
            return "Este compromiso ya está pagado, así que no hay nada más que hacer.";
        }
        if (idEstado == ReglasPagos.ANULADO) {
            return "La empresa dejó este compromiso sin efecto, por eso no tienes nada que pagar.";
        }
        if (idEstado == ReglasPagos.EN_REVISION) {
            return "Este compromiso está en revisión por el reporte que hiciste. "
                    + "Cuando se resuelva, podrás pagarlo si corresponde.";
        }
        return null;
    }

    private String siguienteComprobante() {
        String ultimo = pagos.findTopByOrderByNumeroComprobanteDesc().map(Pago::getNumeroComprobante).orElse(null);
        return ReglasPagos.siguienteNumero(ultimo, "CPR-", "%06d");
    }

    @Transactional
    public Compromiso registrar(RegistrarCompromisoRequest p) {
        UsuarioRemoto ejecutivo = p.idEjecutivo() == null ? null : auth.usuario(p.idEjecutivo()).orElse(null);
        if (ejecutivo == null || ejecutivo.idRol() != ReglasPagos.ROL_EJECUTIVO
                || !Boolean.TRUE.equals(ejecutivo.activo()) || ejecutivo.idEmpresa() == null) {
            throw new NegocioException(HttpStatus.FORBIDDEN,
                    "Solo un ejecutivo activo de una empresa cliente puede registrar compromisos.");
        }
        validarCompromiso(p);
        LocalDate hoy = p.hoy() != null ? p.hoy() : LocalDate.now(reloj);
        LocalDateTime ahora = ahora(p.fechaHora());
        if (p.vencimiento().isBefore(hoy)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento no puede ser anterior a hoy.");
        }
        String concepto = p.concepto().trim();
        String rut = RutUtil.formatear(p.rut());
        UsuarioRemoto persona = auth.porRut(rut).orElse(null);
        if (persona != null && (persona.idRol() != ReglasPagos.ROL_USUARIO_FINAL
                || !Boolean.TRUE.equals(persona.activo()))) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ese RUT no corresponde a un usuario final activo.");
        }
        if (persona == null) {
            persona = crearPersona(p, rut);
        }
        Integer idEmpresa = ejecutivo.idEmpresa();
        if (compromisos.existsByIdUsuarioFinalAndIdEmpresaAndConceptoAndNumeroCuota(
                persona.idUsuario(), idEmpresa, concepto, p.cuota())) {
            throw new NegocioException(HttpStatus.CONFLICT, "Este compromiso ya está registrado para esa persona.");
        }
        Compromiso nuevo = new Compromiso();
        nuevo.setFolio(siguienteFolio(hoy.getYear()));
        nuevo.setIdUsuarioFinal(persona.idUsuario());
        nuevo.setIdEmpresa(idEmpresa);
        nuevo.setConcepto(concepto);
        nuevo.setNumeroCuota(p.cuota());
        nuevo.setTotalCuotas(p.totalCuotas());
        nuevo.setMonto(p.monto());
        nuevo.setFechaVencimiento(p.vencimiento());
        nuevo.setIdEstadoCompromiso(ReglasPagos.PENDIENTE);
        nuevo.setRecargoAplicado(0);
        nuevo.setFechaCambioEstado(ahora);
        nuevo.setIdEjecutivoRegistro(p.idEjecutivo());
        nuevo.setFechaRegistro(ahora);
        return compromisos.save(nuevo);
    }

    private void validarCompromiso(RegistrarCompromisoRequest p) {
        if (p.rut() == null || !RutUtil.esValido(p.rut())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El RUT no es válido.");
        }
        String concepto = p.concepto() == null ? "" : p.concepto().trim();
        if (concepto.isEmpty() || concepto.length() > MAX_CONCEPTO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Escribe el concepto (hasta " + MAX_CONCEPTO + " letras).");
        }
        if (p.monto() == null || p.monto() <= 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor que cero.");
        }
        if (p.vencimiento() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Indica la fecha de vencimiento.");
        }
        if ((p.cuota() == null) != (p.totalCuotas() == null)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Indica el número de cuota y el total de cuotas, o deja ambos vacíos.");
        }
        if (p.cuota() != null && (p.cuota() < 1 || p.totalCuotas() < 1 || p.cuota() > p.totalCuotas())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "La cuota no puede ser mayor que el total de cuotas.");
        }
    }

    private UsuarioRemoto crearPersona(RegistrarCompromisoRequest p, String rut) {
        if (isBlank(p.nombres()) || isBlank(p.apellidos())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Escribe el nombre y los apellidos de la persona.");
        }
        if (p.correo() == null || !CORREO.matcher(p.correo().trim()).matches()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El correo no es válido.");
        }
        if (p.telefono() == null || !TELEFONO.matcher(p.telefono().trim()).matches()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El teléfono no es válido.");
        }
        return auth.crearPersona(new PersonaRemotaRequest(rut, p.nombres().trim(), p.apellidos().trim(),
                p.correo().trim(), p.telefono().trim()));
    }

    private static boolean isBlank(String texto) {
        return texto == null || texto.isBlank();
    }

    private String siguienteFolio(int anio) {
        String prefijo = "CP-" + anio + "-";
        String ultimo = compromisos.findTopByFolioStartingWithOrderByFolioDesc(prefijo)
                .map(Compromiso::getFolio).orElse(null);
        return ReglasPagos.siguienteNumero(ultimo, prefijo, "%04d");
    }

    @Transactional
    public Anulacion anular(Integer idCompromiso, AnularRequest p) {
        UsuarioRemoto ejecutivo = p.idEjecutivo() == null ? null : auth.usuario(p.idEjecutivo()).orElse(null);
        if (ejecutivo == null || ejecutivo.idRol() != ReglasPagos.ROL_EJECUTIVO
                || !Boolean.TRUE.equals(ejecutivo.activo()) || ejecutivo.idEmpresa() == null) {
            throw new NegocioException(HttpStatus.FORBIDDEN,
                    "Solo un ejecutivo activo de una empresa cliente puede anular compromisos.");
        }
        Compromiso c = compromisos.findById(idCompromiso).orElse(null);
        if (c == null || !c.getIdEmpresa().equals(ejecutivo.idEmpresa())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo puedes anular compromisos de tu empresa.");
        }
        if (!ReglasPagos.puedeAnular(c.getIdEstadoCompromiso())) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    c.getIdEstadoCompromiso() == ReglasPagos.PAGADO
                            ? "Un compromiso pagado no se puede anular."
                            : "Este compromiso ya está anulado.");
        }
        if (p.idMotivo() == null || !catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MOTIVO_ANULACION, p.idMotivo())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige el motivo de la anulación.");
        }
        String texto = p.comentario() == null ? "" : p.comentario().trim();
        if (p.idMotivo() == ReglasPagos.MOTIVO_ANULACION_OTRO && texto.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Cuando el motivo es «Otro», escribe un comentario.");
        }
        if (texto.length() > MAX_COMENTARIO_ANULACION) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El comentario puede tener hasta " + MAX_COMENTARIO_ANULACION + " letras.");
        }
        LocalDateTime fecha = ahora(p.fechaHora());
        Anulacion anulacion = new Anulacion();
        anulacion.setIdCompromiso(idCompromiso);
        anulacion.setIdEjecutivo(p.idEjecutivo());
        anulacion.setIdMotivoAnulacion(p.idMotivo());
        anulacion.setComentario(texto.isEmpty() ? null : texto);
        anulacion.setFecha(fecha);
        Anulacion guardada = anulaciones.save(anulacion);
        c.setIdEstadoCompromiso(ReglasPagos.ANULADO);
        c.setFechaCambioEstado(fecha);
        compromisos.save(c);
        return guardada;
    }

    @Transactional
    public int actualizarVencimientos(VencimientosRequest p) {
        LocalDate hoy = p.hoy() != null ? p.hoy() : LocalDate.now(reloj);
        LocalDateTime ahora = ahora(p.fechaHora());
        List<Compromiso> vencidos = compromisos
                .findByIdEstadoCompromisoAndFechaVencimientoBefore(ReglasPagos.PENDIENTE, hoy);
        for (Compromiso c : vencidos) {
            int recargo = empresas.findById(c.getIdEmpresa()).map(EmpresaCliente::getRecargoAtraso).orElse(0);
            c.setIdEstadoCompromiso(ReglasPagos.VENCIDO);
            c.setRecargoAplicado(recargo);
            c.setFechaCambioEstado(ahora);
        }
        compromisos.saveAll(vencidos);
        return vencidos.size();
    }
}
