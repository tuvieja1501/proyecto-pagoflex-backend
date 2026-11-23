package cl.duoc.pagoflex.auth.service;

import cl.duoc.pagoflex.auth.dto.AvisosRequest;
import cl.duoc.pagoflex.auth.dto.ComunaDto;
import cl.duoc.pagoflex.auth.dto.PerfilRequest;
import cl.duoc.pagoflex.auth.dto.PersonaRequest;
import cl.duoc.pagoflex.auth.exception.NegocioException;
import cl.duoc.pagoflex.auth.model.Usuario;
import cl.duoc.pagoflex.auth.repository.ComunaRepository;
import cl.duoc.pagoflex.auth.repository.UsuarioRepository;
import cl.duoc.pagoflex.auth.validacion.RutUtil;
import cl.duoc.pagoflex.auth.validacion.Validaciones;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final int MAX_DIAS_AVISO = 30;

    private final UsuarioRepository usuarios;
    private final ComunaRepository comunas;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarios, ComunaRepository comunas, Clock reloj) {
        this.usuarios = usuarios;
        this.comunas = comunas;
        this.reloj = reloj;
    }

    public Usuario obtener(Integer idUsuario) {
        return usuarios.findById(idUsuario)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No encontramos tu cuenta."));
    }

    public Usuario porRut(String rut) {
        return usuarios.findByRut(RutUtil.formatear(rut))
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "No encontramos a una persona con ese RUT."));
    }

    public List<Usuario> lote(Collection<Integer> ids) {
        return usuarios.findByIdUsuarioIn(ids);
    }

    public List<ComunaDto> comunas() {
        return comunas.findAll().stream().map(c -> new ComunaDto(c.getIdComuna(), c.getNombre())).toList();
    }

    @Transactional
    public Usuario actualizarPerfil(Integer idUsuario, PerfilRequest peticion) {
        Usuario actual = obtener(idUsuario);
        boolean esFinal = actual.getIdRol() == AuthService.ROL_USUARIO_FINAL;
        String error = Validaciones.primerErrorPerfil(peticion.nombres(), peticion.apellidos(), peticion.correo(),
                peticion.telefono(), peticion.idComuna(), esFinal);
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        if (esFinal && !comunas.existsById(peticion.idComuna())) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Elige tu comuna.");
        }
        String correo = peticion.correo().trim();
        usuarios.findByCorreoIgnoreCase(correo)
                .filter(otro -> !otro.getIdUsuario().equals(idUsuario))
                .ifPresent(otro -> {
                    throw new NegocioException(HttpStatus.CONFLICT, "Ese correo ya está usado por otra cuenta.");
                });
        actual.setNombres(peticion.nombres().trim());
        actual.setApellidos(peticion.apellidos().trim());
        actual.setCorreo(correo);
        String telefono = peticion.telefono() == null ? "" : peticion.telefono().trim();
        actual.setTelefono(telefono.isEmpty() ? null : telefono);
        if (esFinal) {
            actual.setIdComuna(peticion.idComuna());
        }
        return usuarios.save(actual);
    }

    @Transactional
    public void guardarAvisos(Integer idUsuario, AvisosRequest peticion) {
        if (peticion.dias() < 1 || peticion.dias() > MAX_DIAS_AVISO) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Los días de anticipación van entre 1 y " + MAX_DIAS_AVISO + ".");
        }
        Usuario actual = obtener(idUsuario);
        actual.setAvisosActivos(peticion.activos());
        actual.setDiasAnticipacion(peticion.dias());
        usuarios.save(actual);
    }

    @Transactional
    public Usuario crearPersona(PersonaRequest peticion) {
        String error = Validaciones.errorRut(peticion.rut());
        if (error == null) {
            error = Validaciones.errorNombre(peticion.nombres(), "los nombres");
        }
        if (error == null) {
            error = Validaciones.errorNombre(peticion.apellidos(), "los apellidos");
        }
        if (error == null) {
            error = Validaciones.errorCorreo(peticion.correo());
        }
        if (error == null) {
            error = Validaciones.errorTelefono(peticion.telefono(), true);
        }
        if (error != null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, error);
        }
        String rut = RutUtil.formatear(peticion.rut());
        if (usuarios.findByRut(rut).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una persona con ese RUT.");
        }
        String correo = peticion.correo().trim();
        if (usuarios.findByCorreoIgnoreCase(correo).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
        Usuario nueva = new Usuario();
        nueva.setIdRol(AuthService.ROL_USUARIO_FINAL);
        nueva.setRut(rut);
        nueva.setNombres(peticion.nombres().trim());
        nueva.setApellidos(peticion.apellidos().trim());
        nueva.setCorreo(correo);
        nueva.setTelefono(peticion.telefono().trim());
        nueva.setActivo(true);
        nueva.setFechaCreacion(LocalDateTime.now(reloj));
        nueva.setFechaIncorporacion(LocalDate.now(reloj));
        nueva.setAvisosActivos(true);
        nueva.setDiasAnticipacion(3);
        return usuarios.save(nueva);
    }
}
