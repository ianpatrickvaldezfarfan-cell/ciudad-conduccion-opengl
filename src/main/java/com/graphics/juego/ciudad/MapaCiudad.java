package com.graphics.juego.ciudad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mapa de la ciudad DATA-DRIVEN: una matriz de caracteres es la unica fuente de verdad.
 * De ella salen el dibujo, el suelo, las colisiones, los limites, las farolas, los
 * semaforos, los destinos, la salida del auto, la camara panoramica y la escala del
 * minimapa. Cambiar el tamano de la matriz adapta todo lo demas automaticamente.
 *
 * Leyenda de cada celda (CELDA x CELDA unidades):
 *   '.'  calle
 *   'T'  cruce de calles con semaforo
 *   'S'  calle donde aparece el auto
 *   '1'..'9' calle con un destino de entrega (en ese orden)
 *   'E'  manzana con edificio        'e' edificio con farola en su esquina
 *   'P'  manzana con parque          'p' parque con farola en su esquina
 *
 * Filas = eje Z (fila 0 = norte, Z negativa); columnas = eje X (columna 0 = oeste).
 */
public class MapaCiudad {

    /** Ancho y profundidad de cada celda: se conserva el tamano del mapa original. */
    public static final float CELDA = 10;

    /*
     * Ciudad ampliada 11x11 (110 x 110 unidades). La esquina noroeste (filas 0-6,
     * columnas 0-6) es el mapa 7x7 original; las columnas 7-10 y las filas 7-10 son
     * los sectores nuevos, conectados por la misma red de calles.
     *   19 manzanas con edificio, 6 parques, 14 farolas, 7 cruces con semaforo, 5 destinos.
     */
    private static final String[] MAPA_BASE = {
            ".........1.",
            ".e.e.P.E.e.",
            "..T...T....",
            "5p.E.e.E.p.",
            "....T...T..",
            ".e.P.E.e.e2",
            "...4..T....",
            ".E.e.E.P.e.",
            "..T...T....",
            "Se.p.E.E.e.",
            ".......3..."
    };

    /** Capa paralela al mapa: a que sector pertenece cada celda. */
    private static final String[] SECTORES_BASE = {
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "CCCCCCCEEEE",
            "SSSSSSSXXXX",
            "SSSSSSSXXXX",
            "SSSSSSSXXXX",
            "SSSSSSSXXXX"
    };

    /** Nombre largo (HUD) y corto (letrero en la calle) de cada sector. */
    public record Sector(char letra, String nombre, String letrero) {
    }

    private static final Sector[] NOMBRES_SECTOR = {
            new Sector('C', "CENTRO HISTORICO", "CENTRO"),
            new Sector('E', "DISTRITO ESTE", "ESTE"),
            new Sector('S', "BARRIO SUR", "SUR"),
            new Sector('X', "ZONA SURESTE", "SURESTE")
    };

    private final char[][] celdas;
    private final char[][] sectores;
    private final int filas;
    private final int columnas;

    public MapaCiudad() {
        this(MAPA_BASE, SECTORES_BASE);
    }

    public MapaCiudad(String[] mapa, String[] capaSectores) {
        filas = mapa.length;
        columnas = mapa[0].length();
        celdas = new char[filas][];
        sectores = new char[filas][];
        for (int f = 0; f < filas; f++) {
            if (mapa[f].length() != columnas || capaSectores[f].length() != columnas) {
                throw new IllegalArgumentException("La fila " + f + " del mapa no tiene " + columnas + " columnas");
            }
            celdas[f] = mapa[f].toCharArray();
            sectores[f] = capaSectores[f].toCharArray();
        }
    }

    // ===================== Sectores =====================

    public Sector sector(int fila, int columna) {
        int f = Math.max(0, Math.min(filas - 1, fila));
        int c = Math.max(0, Math.min(columnas - 1, columna));
        for (Sector s : NOMBRES_SECTOR) {
            if (s.letra() == sectores[f][c]) {
                return s;
            }
        }
        return new Sector('?', "CIUDAD", "CIUDAD");
    }

    public Sector sectorEn(float x, float z) {
        return sector(filaDe(z), columnaDe(x));
    }

    /** Sectores en orden de aparicion (para colocar un letrero por sector). */
    public List<Sector> listaSectores() {
        List<Sector> lista = new ArrayList<>();
        for (Sector s : NOMBRES_SECTOR) {
            lista.add(s);
        }
        return lista;
    }

    /** Primera manzana (no calle) de un sector, recorriendo de norte a sur: {fila, columna}. */
    public int[] primeraManzana(char letraSector) {
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (sectores[f][c] == letraSector && !esCalle(f, c)) {
                    return new int[] { f, c };
                }
            }
        }
        return null;
    }

    // ===================== Dimensiones y coordenadas =====================

    public int filas() {
        return filas;
    }

    public int columnas() {
        return columnas;
    }

    /** Distancia del centro al borde este/oeste. */
    public float limiteX() {
        return columnas * CELDA / 2;
    }

    /** Distancia del centro al borde norte/sur. */
    public float limiteZ() {
        return filas * CELDA / 2;
    }

    /** Mitad del lado mayor: sirve para encuadrar camaras y minimapa. */
    public float limite() {
        return Math.max(limiteX(), limiteZ());
    }

    public float centroX(int columna) {
        return -limiteX() + CELDA * (columna + 0.5f);
    }

    public float centroZ(int fila) {
        return -limiteZ() + CELDA * (fila + 0.5f);
    }

    public int columnaDe(float x) {
        return (int) Math.floor((x + limiteX()) / CELDA);
    }

    public int filaDe(float z) {
        return (int) Math.floor((z + limiteZ()) / CELDA);
    }

    public boolean dentro(int fila, int columna) {
        return fila >= 0 && fila < filas && columna >= 0 && columna < columnas;
    }

    public char celda(int fila, int columna) {
        return celdas[fila][columna];
    }

    // ===================== Tipo de cada celda =====================

    public boolean esCalle(int fila, int columna) {
        if (!dentro(fila, columna)) {
            return false;
        }
        char c = celdas[fila][columna];
        return c == '.' || c == 'T' || c == 'S' || Character.isDigit(c);
    }

    public boolean esEdificio(int fila, int columna) {
        return dentro(fila, columna) && Character.toUpperCase(celdas[fila][columna]) == 'E';
    }

    public boolean esParque(int fila, int columna) {
        return dentro(fila, columna) && Character.toUpperCase(celdas[fila][columna]) == 'P';
    }

    /** Las manzanas en minuscula llevan una farola en su esquina sureste. */
    public boolean tieneFarola(int fila, int columna) {
        return dentro(fila, columna) && !esCalle(fila, columna) && Character.isLowerCase(celdas[fila][columna]);
    }

    public boolean tieneSemaforo(int fila, int columna) {
        return dentro(fila, columna) && celdas[fila][columna] == 'T';
    }

    /** Un cruce es una calle con al menos 3 calles vecinas. */
    public boolean esCruce(int fila, int columna) {
        if (!esCalle(fila, columna)) {
            return false;
        }
        int vecinas = 0;
        for (int d = 0; d < 4; d++) {
            if (esCalle(fila + DF[d], columna + DC[d])) {
                vecinas++;
            }
        }
        return vecinas >= 3;
    }

    /** Direcciones 0 = norte, 1 = este, 2 = sur, 3 = oeste (desplazamiento de fila y columna). */
    public static final int[] DF = { -1, 0, 1, 0 };
    public static final int[] DC = { 0, 1, 0, -1 };

    // ===================== Edificios =====================

    /** Datos de un edificio derivados de su celda (siempre los mismos para la misma celda). */
    public record Edificio(float altura, float alturaBase, float r, float g, float b) {
        public boolean tieneTorre() {
            return alturaBase < altura;
        }
    }

    private static final float[][] PALETA = {
            { 0.55f, 0.42f, 0.36f }, // ladrillo
            { 0.42f, 0.48f, 0.60f }, // azul acero
            { 0.70f, 0.66f, 0.56f }, // arena
            { 0.38f, 0.52f, 0.50f }, // verde agua
            { 0.62f, 0.58f, 0.62f }, // gris lila
            { 0.72f, 0.55f, 0.38f }, // terracota
            { 0.50f, 0.50f, 0.54f }  // concreto
    };

    private static int mezcla(int fila, int columna) {
        int h = fila * 73856093 ^ columna * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    public Edificio edificio(int fila, int columna) {
        int h = mezcla(fila, columna);
        float altura = 6 + h % 13; // entre 6 y 18 unidades
        float[] color = PALETA[(h / 13) % PALETA.length];
        float alturaBase = altura;
        if (altura >= 11 && (h / 91) % 2 == 0) {
            alturaBase = Math.round(altura * 0.6f); // torre mas angosta encima de la base
        }
        return new Edificio(altura, alturaBase, color[0], color[1], color[2]);
    }

    // ===================== Colisiones =====================

    /**
     * true si un circulo de radio "radio" centrado en (x, z) cabe sin tocar ninguna
     * manzana (acera completa de la celda) ni salir del mapa.
     */
    public boolean puedeCircular(float x, float z, float radio) {
        if (Math.abs(x) > limiteX() - radio || Math.abs(z) > limiteZ() - radio) {
            return false;
        }
        int filaAuto = filaDe(z);
        int columnaAuto = columnaDe(x);
        float mitad = CELDA / 2;
        // Con radio < CELDA basta revisar la celda actual y sus 8 vecinas
        for (int f = filaAuto - 1; f <= filaAuto + 1; f++) {
            for (int c = columnaAuto - 1; c <= columnaAuto + 1; c++) {
                if (!dentro(f, c) || esCalle(f, c)) {
                    continue;
                }
                float cx = centroX(c);
                float cz = centroZ(f);
                // Punto del rectangulo mas cercano al centro del circulo
                float cercaX = Math.max(cx - mitad, Math.min(x, cx + mitad));
                float cercaZ = Math.max(cz - mitad, Math.min(z, cz + mitad));
                float dx = x - cercaX;
                float dz = z - cercaZ;
                if (dx * dx + dz * dz < radio * radio) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===================== Elementos derivados del mapa =====================

    /** Posicion de la farola: {posteX, posteZ, bombillaX, bombillaY, bombillaZ}. */
    public List<float[]> farolas() {
        List<float[]> lista = new ArrayList<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (tieneFarola(f, c)) {
                    float posteX = centroX(c) + CELDA / 2 - 0.6f;
                    float posteZ = centroZ(f) + CELDA / 2 - 0.6f;
                    lista.add(new float[] { posteX, posteZ, posteX + 1.1f, 5.6f, posteZ + 1.1f });
                }
            }
        }
        return lista;
    }

    /** Destinos ordenados por su digito: cada elemento es {x, z}. */
    public List<float[]> destinos() {
        Map<Character, float[]> ordenados = new TreeMap<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (Character.isDigit(celdas[f][c])) {
                    ordenados.put(celdas[f][c], new float[] { centroX(c), centroZ(f) });
                }
            }
        }
        return new ArrayList<>(ordenados.values());
    }

    /** Salida del auto: {x, z, angulo}. El angulo apunta hacia una calle vecina (prefiere el norte). */
    public float[] salida() {
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (celdas[f][c] == 'S') {
                    float angulo = 0;
                    // angulo 0 = norte, PI/2 = oeste, PI = sur, -PI/2 = este
                    float[] angulos = { 0, (float) (-Math.PI / 2), (float) Math.PI, (float) (Math.PI / 2) };
                    for (int d = 0; d < 4; d++) {
                        if (esCalle(f + DF[d], c + DC[d])) {
                            angulo = angulos[d];
                            break;
                        }
                    }
                    return new float[] { centroX(c), centroZ(f), angulo };
                }
            }
        }
        throw new IllegalStateException("El mapa no tiene una celda de salida 'S'");
    }

    /** Cuenta las celdas de un tipo (para comprobar los requisitos del mapa). */
    public int contar(char tipoMayuscula) {
        int total = 0;
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (Character.toUpperCase(celdas[f][c]) == tipoMayuscula) {
                    total++;
                }
            }
        }
        return total;
    }
}
