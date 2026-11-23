package cl.duoc.pagoflex.pagos.service;

import cl.duoc.pagoflex.pagos.client.AuthGateway;
import cl.duoc.pagoflex.pagos.dto.NuevoReporteRequest;
import cl.duoc.pagoflex.pagos.dto.ResolverReporteRequest;
import cl.duoc.pagoflex.pagos.dto.TomarReporteRequest;
import cl.duoc.pagoflex.pagos.dto.UsuarioRemoto;
import cl.duoc.pagoflex.pagos.exception.NegocioException;
import cl.duoc.pagoflex.pagos.model.Compromiso;
import cl.duoc.pagoflex.pagos.model.EmpresaCliente;
import cl.duoc.pagoflex.pagos.model.Reporte;
import cl.duoc.pagoflex.pagos.repository.CatalogoRepository;
import cl.duoc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duoc.pagoflex.pagos.repository.EmpresaRepository;
import cl.duoc.pagoflex.pagos.repository.ReporteRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    public static final int MAX_COMENTARIO = 500;
    public static final int MAX_RESPUESTA = 500;

    private final ReporteRepository reportes;
    private final CompromisoRepository compromisos;
    private final EmpresaRepository empresas;
    private final CatalogoRepository catalogos;
    private final AuthGateway auth;
    private final Clock reloj;

    public ReporteService(ReporteRepository reportes, CompromisoRepository compromisos, EmpresaRepository empresas,
                          CatalogoRepository catalogos, AuthGateway auth, Clock reloj) {
        this.reportes = reportes;
        this.compromisos = compromisos;
        this.empresas = empresas;
        this.catalogos = catalogos;
        this.auth = auth;
        this.reloj = reloj;
    }

    private LocalDateTime ahora(LocalDateTime pedido) {
        return pedido != null ? pedido : LocalDateTime.now(reloj);
    }

    public List<Reporte> listar(Integer idUsuario, Integer idCompromiso) {
        if (idCompromiso != null) {
            return reportes.findByIdCompromisoIn(List.of(idCompromiso));
        }
        if (idUsuario != null) {
            List<Integer> ids = compromisos.findByIdUsuarioFinalOrderByFechaVencimiento(idUsuario).stream()
                    .map(Compromiso::getIdCompromiso).toList();
            return reportes.findByIdCompromisoIn(ids);
        }
        return reportes.findAll();
    }

    public Reporte obtener(Integer id) {
        return reportes.findById(id)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No encontramos el reporte."));
    }

    @Transactional
    public Reporte crear(NuevoReporteRequest p) {
        String texto = p.comentario() == null ? "" : p.comentario().trim();
        if (p.idMotivo() == null || !catalogos.existsByTipoAndCodigo(ReglasPagos.TIPO_MOTIVO_REPORTE, p.idMotivo())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige el motivo de tu reporte.");
        }
        if (p.idMotivo() == ReglasPagos.MOTIVO_REPORTE_OTRO && texto.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Cuéntanos qué pasó para poder ayudarte.");
        }
        if (texto.length() > MAX_COMENTARIO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "El comentario puede tener hasta " + MAX_COMENTARIO + " letras.");
        }
        Compromiso c = p.idCompromiso() == null ? null : compromisos.findById(p.idCompromiso()).orElse(null);
        if (p.idUsuario() == null || c == null || !c.getIdUsuarioFinal().equals(p.idUsuario())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Este compromiso no corresponde a tu cuenta.");
        }
        reportes.findFirstByIdCompromisoAndIdEstadoReporteNot(p.idCompromiso(), ReglasPagos.REPORTE_RESUELTO)
                .ifPresent(vigente -> {
                    throw new NegocioException(HttpStatus.CONFLICT,
                            "Este compromiso ya tiene un reporte en curso (" + vigente.getNumero() + ").");
                });
        if (!ReglasPagos.puedeReportar(c.getIdEstadoCompromiso())) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Solo se pueden reportar compromisos pendientes o vencidos.");
        }
        LocalDateTime fecha = ahora(p.fechaHora());
        Reporte nuevo = new Reporte();
        nuevo.setNumero(siguienteNumero());
        nuevo.setIdCompromiso(p.idCompromiso());
        nuevo.setIdMotivoReporte(p.idMotivo());
        nuevo.setComentario(texto.isEmpty() ? null : texto);
        nuevo.setFechaCreacion(fecha);
        nuevo.setIdEstadoReporte(ReglasPagos.REPORTE_ABIERTO);
        Reporte guardado = reportes.save(nuevo);
        c.setIdEstadoCompromiso(ReglasPagos.EN_REVISION);
        c.setFechaCambioEstado(fecha);
        compromisos.save(c);
        return guardado;
    }

    private String siguienteNumero() {
        String ultimo = reportes.findTopByOrderByNumeroDesc().map(Reporte::getNumero).orElse(null);
        return ReglasPagos.siguienteNumero(ultimo, "REP-", "%04d");
    }

    private void exigirAgenteActivo(Integer idAgente) {
        UsuarioRemoto agente = idAgente == null ? null : auth.usuario(idAgente).orElse(null);
        if (agente == null || agente.idRol() != ReglasPagos.ROL_AGENTE || !Boolean.TRUE.equals(agente.activo())) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "Solo un agente PagoFlex activo puede gestionar reportes.");
        }
    }

    @Transactional
    public Reporte tomar(Integer idReporte, TomarReporteRequest p) {
        exigirAgenteActivo(p.idAgente());
        Reporte r = obtener(idReporte);
        if (r.getIdEstadoReporte() != ReglasPagos.REPORTE_ABIERTO) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    r.getIdEstadoReporte() == ReglasPagos.REPORTE_EN_GESTION
                            ? "Este reporte ya está en gestión."
                            : "Este reporte ya está resuelto.");
        }
        r.setIdEstadoReporte(ReglasPagos.REPORTE_EN_GESTION);
        r.setIdAgente(p.idAgente());
        r.setFechaToma(ahora(p.fechaHora()));
        return reportes.save(r);
    }

    @Transactional
    public Compromiso resolver(Integer idReporte, ResolverReporteRequest p) {
        exigirAgenteActivo(p.idAgente());
        Reporte r = obtener(idReporte);
        if (r.getIdEstadoReporte() != ReglasPagos.REPORTE_EN_GESTION) {
            throw new NegocioException(HttpStatus.CONFLICT, "Primero toma el reporte para poder resolverlo.");
        }
        if (!p.idAgente().equals(r.getIdAgente())) {
            String responsable = auth.usuario(r.getIdAgente()).map(UsuarioRemoto::nombreCompleto).orElse("otro agente");
            throw new NegocioException(HttpStatus.CONFLICT, "Este reporte lo está gestionando " + responsable + ".");
        }
        String texto = p.respuesta() == null ? "" : p.respuesta().trim();
        if (texto.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Escribe la respuesta para la persona antes de resolver el reporte.");
        }
        if (texto.length() > MAX_RESPUESTA) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "La respuesta puede tener hasta " + MAX_RESPUESTA + " letras.");
        }
        Compromiso c = compromisos.findById(r.getIdCompromiso())
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "No encontramos el compromiso del reporte."));
        LocalDateTime fecha = ahora(p.fechaHora());
        LocalDate hoy = p.hoy() != null ? p.hoy() : LocalDate.now(reloj);
        int nuevoEstado = ReglasPagos.estadoTrasResolver(c.getIdEstadoCompromiso(), c.getFechaVencimiento(), hoy);
        int recargo = c.getRecargoAplicado();
        if (nuevoEstado == ReglasPagos.VENCIDO && recargo == 0) {
            recargo = empresas.findById(c.getIdEmpresa()).map(EmpresaCliente::getRecargoAtraso).orElse(0);
        }
        if (nuevoEstado != c.getIdEstadoCompromiso() || recargo != c.getRecargoAplicado()) {
            c.setIdEstadoCompromiso(nuevoEstado);
            c.setRecargoAplicado(recargo);
            c.setFechaCambioEstado(fecha);
            compromisos.save(c);
        }
        r.setIdEstadoReporte(ReglasPagos.REPORTE_RESUELTO);
        r.setRespuesta(texto);
        r.setFechaResolucion(fecha);
        reportes.save(r);
        return c;
    }
}
