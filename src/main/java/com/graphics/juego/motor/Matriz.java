package com.graphics.juego.motor;

/**
 * Operaciones con matrices 4x4 guardadas como float[16] en orden de COLUMNAS,
 * que es el formato que espera glUniformMatrix4fv con transpose = false.
 * El elemento (fila, columna) esta en el indice columna * 4 + fila.
 */
public final class Matriz {

    private Matriz() {
    }

    /** Proyeccion en perspectiva: lo lejano se ve mas pequeno (vista principal). */
    public static float[] perspectiva(float fovYGrados, float aspecto, float cerca, float lejos) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fovYGrados) / 2.0));
        float[] m = new float[16];
        m[0] = f / aspecto;
        m[5] = f;
        m[10] = (lejos + cerca) / (cerca - lejos);
        m[11] = -1;
        m[14] = 2 * lejos * cerca / (cerca - lejos);
        return m;
    }

    /** Proyeccion ortografica: no hay perspectiva, se usa en el minimapa y en el HUD. */
    public static float[] ortografica(float izq, float der, float abajo, float arriba, float cerca, float lejos) {
        float[] m = new float[16];
        m[0] = 2 / (der - izq);
        m[5] = 2 / (arriba - abajo);
        m[10] = -2 / (lejos - cerca);
        m[12] = -(der + izq) / (der - izq);
        m[13] = -(arriba + abajo) / (arriba - abajo);
        m[14] = -(lejos + cerca) / (lejos - cerca);
        m[15] = 1;
        return m;
    }

    /** Matriz de vista: coloca la camara en "ojo" mirando hacia "objetivo". */
    public static float[] mirarA(float ojoX, float ojoY, float ojoZ,
            float objX, float objY, float objZ,
            float arribaX, float arribaY, float arribaZ) {
        // f = direccion hacia donde mira la camara
        float fx = objX - ojoX;
        float fy = objY - ojoY;
        float fz = objZ - ojoZ;
        float lf = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= lf;
        fy /= lf;
        fz /= lf;
        // s = derecha de la camara = f x arriba
        float sx = fy * arribaZ - fz * arribaY;
        float sy = fz * arribaX - fx * arribaZ;
        float sz = fx * arribaY - fy * arribaX;
        float ls = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= ls;
        sy /= ls;
        sz /= ls;
        // u = arriba real de la camara = s x f
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        float[] m = new float[16];
        m[0] = sx;
        m[4] = sy;
        m[8] = sz;
        m[1] = ux;
        m[5] = uy;
        m[9] = uz;
        m[2] = -fx;
        m[6] = -fy;
        m[10] = -fz;
        m[12] = -(sx * ojoX + sy * ojoY + sz * ojoZ);
        m[13] = -(ux * ojoX + uy * ojoY + uz * ojoZ);
        m[14] = fx * ojoX + fy * ojoY + fz * ojoZ;
        m[15] = 1;
        return m;
    }

    /** Devuelve a * b (primero se aplica b y luego a). */
    public static float[] multiplicar(float[] a, float[] b) {
        float[] r = new float[16];
        for (int columna = 0; columna < 4; columna++) {
            for (int fila = 0; fila < 4; fila++) {
                float suma = 0;
                for (int k = 0; k < 4; k++) {
                    suma += a[k * 4 + fila] * b[columna * 4 + k];
                }
                r[columna * 4 + fila] = suma;
            }
        }
        return r;
    }
}
