package cl.duoc.pagoflex.recomendaciones.service;

import java.util.List;

public final class ReglasRecomendacion {

    public static final int ROL_USUARIO_FINAL = 1;
    public static final int ROL_AGENTE = 2;

    public static final int RECIBIDA = 1;
    public static final int CONTACTADA = 2;
    public static final int EN_EVALUACION = 3;
    public static final int SE_UNIO = 4;
    public static final int DESCARTADA = 5;

    public static final int MAX_POR_MES = 5;
    public static final int MAX_MOTIVO = 255;

    public static final String TIPO_RUBRO = "RUBRO";
    public static final String TIPO_ESTADO = "ESTADO_RECOMENDACION";

    private ReglasRecomendacion() {
    }

    public static boolean esFinal(int idEstado) {
        return idEstado == SE_UNIO || idEstado == DESCARTADA;
    }

    public static List<Integer> estadosSiguientes(int idEstado) {
        return switch (idEstado) {
            case RECIBIDA -> List.of(CONTACTADA);
            case CONTACTADA -> List.of(EN_EVALUACION);
            case EN_EVALUACION -> List.of(SE_UNIO, DESCARTADA);
            default -> List.of();
        };
    }

    public static boolean puedeCambiar(int actual, int nuevo) {
        return estadosSiguientes(actual).contains(nuevo);
    }

    public static String normalizarNombre(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    public static String siguienteNumero(String ultimo) {
        int actual = 0;
        if (ultimo != null && ultimo.startsWith("REC-")) {
            try {
                actual = Integer.parseInt(ultimo.substring(4));
            } catch (NumberFormatException e) {
                actual = 0;
            }
        }
        return String.format("REC-%04d", actual + 1);
    }
}
