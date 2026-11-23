package cl.duoc.pagoflex.pagos.service;

import java.time.LocalDate;

public final class ReglasPagos {

    public static final int ROL_USUARIO_FINAL = 1;
    public static final int ROL_AGENTE = 2;
    public static final int ROL_EJECUTIVO = 3;

    public static final int PENDIENTE = 1;
    public static final int VENCIDO = 2;
    public static final int PAGADO = 3;
    public static final int EN_REVISION = 4;
    public static final int ANULADO = 5;

    public static final int REPORTE_ABIERTO = 1;
    public static final int REPORTE_EN_GESTION = 2;
    public static final int REPORTE_RESUELTO = 3;

    public static final int CANAL_APP = 1;
    public static final int MOTIVO_REPORTE_OTRO = 4;
    public static final int MOTIVO_ANULACION_OTRO = 4;

    public static final String TIPO_ESTADO_COMPROMISO = "ESTADO_COMPROMISO";
    public static final String TIPO_MOTIVO_ANULACION = "MOTIVO_ANULACION";
    public static final String TIPO_CANAL = "CANAL";
    public static final String TIPO_MEDIO_PAGO = "MEDIO_PAGO";
    public static final String TIPO_MOTIVO_REPORTE = "MOTIVO_REPORTE";
    public static final String TIPO_ESTADO_REPORTE = "ESTADO_REPORTE";

    private ReglasPagos() {
    }

    public static boolean puedePagar(int idEstado) {
        return idEstado == PENDIENTE || idEstado == VENCIDO;
    }

    public static boolean puedeReportar(int idEstado) {
        return puedePagar(idEstado);
    }

    public static boolean puedeAnular(int idEstado) {
        return idEstado == PENDIENTE || idEstado == VENCIDO || idEstado == EN_REVISION;
    }

    public static int estadoTrasResolver(int idEstadoActual, LocalDate vencimiento, LocalDate hoy) {
        if (idEstadoActual == ANULADO) {
            return ANULADO;
        }
        if (idEstadoActual == PAGADO) {
            return PAGADO;
        }
        return vencimiento.isBefore(hoy) ? VENCIDO : PENDIENTE;
    }

    public static String siguienteNumero(String ultimo, String prefijo, String formato) {
        int actual = 0;
        if (ultimo != null && ultimo.startsWith(prefijo)) {
            try {
                actual = Integer.parseInt(ultimo.substring(prefijo.length()));
            } catch (NumberFormatException e) {
                actual = 0;
            }
        }
        return prefijo + String.format(formato, actual + 1);
    }
}
