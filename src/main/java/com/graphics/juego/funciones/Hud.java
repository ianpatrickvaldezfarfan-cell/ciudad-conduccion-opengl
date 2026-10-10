package com.graphics.juego.funciones;

import static org.lwjgl.opengl.GL33.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import com.graphics.juego.motor.Fuente;

/**
 * Dibujo 2D superpuesto (HUD): rectangulos semitransparentes y texto.
 *
 * Tiene su propio programa de shaders y un VBO dinamico. Las coordenadas son
 * pixeles con (0, 0) en la esquina superior izquierda. El texto usa una textura
 * generada en memoria a partir de la fuente 5x7 (no se cargan imagenes).
 * Uso: comenzar() -> rect()/texto() -> terminar().
 */
public class Hud {

    private static final int CELDA_X = Fuente.ANCHO + 1; // 1 pixel de separacion
    private static final int CELDA_Y = Fuente.ALTO + 1;
    private static final int POR_FILA = 16;
    private static final int FLOATS_POR_VERTICE = 8; // x, y, u, v, r, g, b, a
    private static final int MAX_VERTICES = 60000;

    private int programa;
    private int vao;
    private int vbo;
    private int textura;
    private int ubicacionPantalla;
    private int anchoAtlas;
    private int altoAtlas;
    private final Map<Character, Integer> indice = new HashMap<>();
    private final FloatBuffer datos = BufferUtils.createFloatBuffer(MAX_VERTICES * FLOATS_POR_VERTICE);
    private int ancho;
    private int alto;

    public void crear() {
        crearPrograma();
        crearTexturaFuente();
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_VERTICES * FLOATS_POR_VERTICE * Float.BYTES, GL_DYNAMIC_DRAW);
        int stride = FLOATS_POR_VERTICE * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void destruir() {
        glDeleteProgram(programa);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteTextures(textura);
    }

    // ===================== Lote de dibujo =====================

    public void comenzar(int anchoPantalla, int altoPantalla) {
        ancho = anchoPantalla;
        alto = altoPantalla;
        datos.clear();
    }

    /** Envia todo el lote a la GPU de una vez y restaura el estado 3D. */
    public void terminar() {
        int vertices = datos.position() / FLOATS_POR_VERTICE;
        if (vertices == 0) {
            return;
        }
        datos.flip();
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(programa);
        glUniform2f(ubicacionPantalla, ancho, alto);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textura);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, datos);
        glDrawArrays(GL_TRIANGLES, 0, vertices);
        glBindVertexArray(0);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    /** Rectangulo de color solido (alfa entre 0 y 1). */
    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        quad(x, y, w, h, -1, -1, -1, -1, r, g, b, a);
    }

    /** Ancho en pixeles que ocupa un texto con la escala dada. */
    public static float anchoTexto(String texto, float escala) {
        return texto.length() * CELDA_X * escala - escala;
    }

    public static float altoTexto(float escala) {
        return Fuente.ALTO * escala;
    }

    /** Texto con una sombra oscura para que se lea sobre cualquier fondo. */
    public void texto(float x, float y, float escala, String texto, float r, float g, float b) {
        textoPlano(x + Math.max(1, escala / 2), y + Math.max(1, escala / 2), escala, texto, 0, 0, 0, 0.75f);
        textoPlano(x, y, escala, texto, r, g, b, 1);
    }

    public void textoPlano(float x, float y, float escala, String texto, float r, float g, float b, float a) {
        float cursor = x;
        for (int i = 0; i < texto.length(); i++) {
            char c = Fuente.normalizar(texto.charAt(i));
            if (c != ' ') {
                int n = indice.get(c);
                float u0 = (float) (n % POR_FILA) * CELDA_X / anchoAtlas;
                float v0 = (float) (n / POR_FILA) * CELDA_Y / altoAtlas;
                float u1 = u0 + (float) CELDA_X / anchoAtlas;
                float v1 = v0 + (float) CELDA_Y / altoAtlas;
                quad(cursor, y, CELDA_X * escala, CELDA_Y * escala, u0, v0, u1, v1, r, g, b, a);
            }
            cursor += CELDA_X * escala;
        }
    }

    private void quad(float x, float y, float w, float h, float u0, float v0, float u1, float v1,
            float r, float g, float b, float a) {
        if (datos.remaining() < 6 * FLOATS_POR_VERTICE) {
            return; // lote lleno: se omite (no deberia ocurrir con este HUD)
        }
        vertice(x, y, u0, v0, r, g, b, a);
        vertice(x + w, y, u1, v0, r, g, b, a);
        vertice(x + w, y + h, u1, v1, r, g, b, a);
        vertice(x, y, u0, v0, r, g, b, a);
        vertice(x + w, y + h, u1, v1, r, g, b, a);
        vertice(x, y + h, u0, v1, r, g, b, a);
    }

    private void vertice(float x, float y, float u, float v, float r, float g, float b, float a) {
        datos.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    // ===================== Recursos de la GPU =====================

    private void crearTexturaFuente() {
        char[] caracteres = Fuente.caracteres();
        int filas = (caracteres.length + POR_FILA - 1) / POR_FILA;
        anchoAtlas = POR_FILA * CELDA_X;
        altoAtlas = filas * CELDA_Y;
        ByteBuffer pixeles = BufferUtils.createByteBuffer(anchoAtlas * altoAtlas);
        for (int n = 0; n < caracteres.length; n++) {
            char c = caracteres[n];
            indice.put(c, n);
            int ox = (n % POR_FILA) * CELDA_X;
            int oy = (n / POR_FILA) * CELDA_Y;
            for (int fila = 0; fila < Fuente.ALTO; fila++) {
                for (int col = 0; col < Fuente.ANCHO; col++) {
                    if (Fuente.pixel(c, col, fila)) {
                        pixeles.put((oy + fila) * anchoAtlas + ox + col, (byte) 255);
                    }
                }
            }
        }
        textura = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textura);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, anchoAtlas, altoAtlas, 0, GL_RED, GL_UNSIGNED_BYTE, pixeles);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void crearPrograma() {
        String vertex = """
                #version 330 core
                layout (location = 0) in vec2 aPos;   // pixeles, (0,0) arriba a la izquierda
                layout (location = 1) in vec2 aUv;    // coordenada en la textura (negativa = sin textura)
                layout (location = 2) in vec4 aColor;
                uniform vec2 uPantalla;
                out vec2 vUv;
                out vec4 vColor;
                void main() {
                    vec2 ndc = vec2(aPos.x / uPantalla.x * 2.0 - 1.0, 1.0 - aPos.y / uPantalla.y * 2.0);
                    gl_Position = vec4(ndc, 0.0, 1.0);
                    vUv = aUv;
                    vColor = aColor;
                }
                """;
        String fragment = """
                #version 330 core
                in vec2 vUv;
                in vec4 vColor;
                uniform sampler2D uFuente;
                out vec4 color;
                void main() {
                    float cobertura = vUv.x < 0.0 ? 1.0 : texture(uFuente, vUv).r;
                    color = vec4(vColor.rgb, vColor.a * cobertura);
                }
                """;
        int vs = compilar(GL_VERTEX_SHADER, vertex);
        int fs = compilar(GL_FRAGMENT_SHADER, fragment);
        programa = glCreateProgram();
        glAttachShader(programa, vs);
        glAttachShader(programa, fs);
        glLinkProgram(programa);
        glDeleteShader(vs);
        glDeleteShader(fs);
        if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("HUD: " + glGetProgramInfoLog(programa));
        }
        ubicacionPantalla = glGetUniformLocation(programa, "uPantalla");
        glUseProgram(programa);
        glUniform1i(glGetUniformLocation(programa, "uFuente"), 0);
    }

    private static int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("HUD shader: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
