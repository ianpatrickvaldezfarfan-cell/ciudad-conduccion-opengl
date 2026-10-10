package com.graphics.juego.motor;

import java.util.HashMap;
import java.util.Map;

/**
 * Fuente de pixeles de 5 x 7 escrita a mano (sin imagenes externas).
 * La usan el HUD (como textura) y los letreros 3D de la ciudad (como cajas).
 * Cada letra son 7 filas de 5 caracteres: '#' = pixel encendido.
 */
public final class Fuente {

    public static final int ANCHO = 5;
    public static final int ALTO = 7;

    private static final String[] DATOS = {
            "A .###. #...# #...# ##### #...# #...# #...#",
            "B ####. #...# #...# ####. #...# #...# ####.",
            "C .###. #...# #.... #.... #.... #...# .###.",
            "D ####. #...# #...# #...# #...# #...# ####.",
            "E ##### #.... #.... ####. #.... #.... #####",
            "F ##### #.... #.... ####. #.... #.... #....",
            "G .###. #...# #.... #.### #...# #...# .###.",
            "H #...# #...# #...# ##### #...# #...# #...#",
            "I .###. ..#.. ..#.. ..#.. ..#.. ..#.. .###.",
            "J ..### ...#. ...#. ...#. ...#. #..#. .##..",
            "K #...# #..#. #.#.. ##... #.#.. #..#. #...#",
            "L #.... #.... #.... #.... #.... #.... #####",
            "M #...# ##.## #.#.# #.#.# #...# #...# #...#",
            "N #...# #...# ##..# #.#.# #..## #...# #...#",
            "O .###. #...# #...# #...# #...# #...# .###.",
            "P ####. #...# #...# ####. #.... #.... #....",
            "Q .###. #...# #...# #...# #.#.# #..#. .##.#",
            "R ####. #...# #...# ####. #.#.. #..#. #...#",
            "S .#### #.... #.... .###. ....# ....# ####.",
            "T ##### ..#.. ..#.. ..#.. ..#.. ..#.. ..#..",
            "U #...# #...# #...# #...# #...# #...# .###.",
            "V #...# #...# #...# #...# #...# .#.#. ..#..",
            "W #...# #...# #...# #.#.# #.#.# #.#.# .#.#.",
            "X #...# #...# .#.#. ..#.. .#.#. #...# #...#",
            "Y #...# #...# .#.#. ..#.. ..#.. ..#.. ..#..",
            "Z ##### ....# ...#. ..#.. .#... #.... #####",
            "0 .###. #...# #..## #.#.# ##..# #...# .###.",
            "1 ..#.. .##.. ..#.. ..#.. ..#.. ..#.. .###.",
            "2 .###. #...# ....# ...#. ..#.. .#... #####",
            "3 ##### ...#. ..#.. ...#. ....# #...# .###.",
            "4 ...#. ..##. .#.#. #..#. ##### ...#. ...#.",
            "5 ##### #.... ####. ....# ....# #...# .###.",
            "6 ..##. .#... #.... ####. #...# #...# .###.",
            "7 ##### ....# ...#. ..#.. .#... .#... .#...",
            "8 .###. #...# #...# .###. #...# #...# .###.",
            "9 .###. #...# #...# .#### ....# ...#. .##..",
            ": ..... .##.. .##.. ..... .##.. .##.. .....",
            ". ..... ..... ..... ..... ..... .##.. .##..",
            ", ..... ..... ..... ..... .##.. ..#.. .#...",
            "- ..... ..... ..... ##### ..... ..... .....",
            "+ ..... ..#.. ..#.. ##### ..#.. ..#.. .....",
            "/ ..... ....# ...#. ..#.. .#... #.... .....",
            "! ..#.. ..#.. ..#.. ..#.. ..#.. ..... ..#..",
            "? .###. #...# ....# ...#. ..#.. ..... ..#..",
            "( ...#. ..#.. .#... .#... .#... ..#.. ...#.",
            ") .#... ..#.. ...#. ...#. ...#. ..#.. .#...",
            "| ..#.. ..#.. ..#.. ..#.. ..#.. ..#.. ..#..",
            "% ##... ##..# ...#. ..#.. .#... #..## ...##",
            "= ..... ..... ##### ..... ##### ..... .....",
            "< ...#. ..#.. .#... #.... .#... ..#.. ...#.",
            "> .#... ..#.. ...#. ....# ...#. ..#.. .#...",
            "^ ..#.. .###. #.#.# ..#.. ..#.. ..#.. ..#..",
            "_ ..... ..... ..... ..... ..... ..... #####"
    };

    private static final Map<Character, boolean[][]> GLIFOS = new HashMap<>();

    static {
        for (String linea : DATOS) {
            String[] partes = linea.split(" ");
            boolean[][] g = new boolean[ALTO][ANCHO];
            for (int fila = 0; fila < ALTO; fila++) {
                for (int col = 0; col < ANCHO; col++) {
                    g[fila][col] = partes[fila + 1].charAt(col) == '#';
                }
            }
            GLIFOS.put(partes[0].charAt(0), g);
        }
        GLIFOS.put(' ', new boolean[ALTO][ANCHO]);
    }

    private Fuente() {
    }

    /** Pasa a mayusculas y quita tildes: la fuente solo tiene mayusculas sin acento. */
    public static char normalizar(char c) {
        c = Character.toUpperCase(c);
        switch (c) {
            case 'Á', 'À' -> c = 'A';
            case 'É', 'È' -> c = 'E';
            case 'Í', 'Ì' -> c = 'I';
            case 'Ó', 'Ò' -> c = 'O';
            case 'Ú', 'Ù', 'Ü' -> c = 'U';
            case 'Ñ' -> c = 'N';
            default -> {
            }
        }
        return GLIFOS.containsKey(c) ? c : '?';
    }

    /** true si el pixel (columna, fila) de la letra esta encendido; fila 0 = arriba. */
    public static boolean pixel(char c, int columna, int fila) {
        return GLIFOS.get(normalizar(c))[fila][columna];
    }

    /** Todos los caracteres disponibles (para construir la textura del HUD). */
    public static char[] caracteres() {
        char[] lista = new char[GLIFOS.size()];
        int i = 0;
        for (char c : GLIFOS.keySet()) {
            lista[i++] = c;
        }
        java.util.Arrays.sort(lista);
        return lista;
    }
}
