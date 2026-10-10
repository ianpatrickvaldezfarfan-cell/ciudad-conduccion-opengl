package com.graphics.juego.motor;

import static org.lwjgl.opengl.GL33.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Motor de dibujo 3D: compila los shaders, guarda el cubo unitario en la GPU (VAO/VBO)
 * y ofrece caja()/cajaGirada() para dibujar cualquier objeto de la ciudad.
 *
 * Toda la escena se construye con un unico cubo de lado 1 al que el vertex shader
 * aplica escala, giro alrededor de Y y traslacion. La camara llega como una sola
 * matriz uVistaProy (proyeccion * vista), asi la vista principal usa perspectiva y
 * el minimapa usa una proyeccion ortografica propia sin tocar el shader.
 */
public class Renderizador {

    /** Numero maximo de farolas que el shader puede recibir. */
    public static final int MAX_FAROLAS = 16;

    private int programa;
    private int vao;
    private int vbo;
    private final Map<String, Integer> uniforms = new HashMap<>();

    /** Crea el programa de shaders y el cubo. Requiere un contexto OpenGL activo. */
    public void crear() {
        crearPrograma();
        crearCubo();
    }

    /** Activa el programa y el VAO del cubo antes de dibujar. */
    public void usar() {
        glUseProgram(programa);
        glBindVertexArray(vao);
    }

    /** Envia la matriz de camara (proyeccion * vista). */
    public void vistaProyeccion(float[] matriz) {
        glUniformMatrix4fv(uniform("uVistaProy"), false, matriz);
    }

    /**
     * Emision = la superficie muestra su propio color sin recibir luz (bombillas,
     * ventanas encendidas, minimapa). No ilumina a otros objetos: eso lo hacen las
     * luces que se envian a uFarolas / uFaroPos.
     */
    public void emision(boolean activa) {
        entero("uEmision", activa ? 1 : 0);
    }

    /** Distancias donde empieza y termina la niebla (valores enormes = sin niebla). */
    public void niebla(float inicio, float fin) {
        decimal("uNieblaInicio", inicio);
        decimal("uNieblaFin", fin);
    }

    // ===================== Dibujo de cajas =====================

    /** Caja sin giro: posicion XYZ (centro), tamano XYZ y color RGB entre 0 y 1. */
    public void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    /** Caja con giro (radianes) alrededor del eje Y. */
    public void cajaGirada(float x, float y, float z, float sx, float sy, float sz,
            float r, float g, float b, float angulo) {
        vector("uPos", x, y, z);
        vector("uEscala", sx, sy, sz);
        vector("uColor", r, g, b);
        decimal("uGiro", angulo);
        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    // ===================== Envio de uniforms =====================

    /** Busca un uniform una sola vez y guarda su ubicacion (-1 si el shader no lo usa). */
    private int uniform(String nombre) {
        Integer ubicacion = uniforms.get(nombre);
        if (ubicacion == null) {
            ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre, ubicacion);
        }
        return ubicacion;
    }

    public void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    public void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    public void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor);
    }

    /** Libera la GPU. */
    public void destruir() {
        if (programa != 0) {
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
    }

    // ===================== Shaders =====================
    // IMPORTANTE: dentro del codigo GLSL no usar tildes ni la letra enie,
    // algunos drivers de Windows (Intel/AMD) no compilan caracteres fuera de ASCII.

    private static String vertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec3 aPos;    // posicion local del vertice del cubo
                layout (location = 1) in vec3 aNormal; // normal de la cara
                uniform vec3 uPos;       // centro de la caja en el mundo
                uniform vec3 uEscala;    // tamano de la caja en cada eje
                uniform float uGiro;     // giro alrededor de Y en radianes
                uniform mat4 uVistaProy; // proyeccion * vista de la camara activa
                out vec3 vMundo;         // posicion en el mundo para la iluminacion
                out vec3 vNormal;        // normal en el mundo para la iluminacion

                void main() {
                    float c = cos(uGiro);
                    float s = sin(uGiro);
                    mat3 giro = mat3(c, 0.0, -s,   // columna 0
                                     0.0, 1.0, 0.0, // columna 1
                                     s, 0.0, c);   // columna 2
                    vMundo = giro * (aPos * uEscala) + uPos;
                    // Inversa transpuesta de escala + giro: mantiene la normal perpendicular
                    vNormal = normalize(giro * (aNormal / uEscala));
                    gl_Position = uVistaProy * vec4(vMundo, 1.0);
                }
                """;
    }

    /**
     * Iluminacion por fragmento (modelo de Lambert):
     *   luz = ambiente + sol * max(dot(N, L), 0)
     *       + suma de farolas (luz puntual con atenuacion por distancia)
     *       + dos faros (focos con cono, atenuacion y alcance limitado)
     * El color final es colorMaterial * luz, con niebla segun la distancia a la camara.
     */
    private static String fragmentShader() {
        return """
                #version 330 core
                in vec3 vMundo;
                in vec3 vNormal;

                uniform vec3 uColor;     // color del material
                uniform int uEmision;    // 1 = color propio (bombilla, ventana encendida, mapa)

                uniform vec3 uAmbiente;  // luz que llega a todas las caras
                uniform vec3 uSolDir;    // direccion HACIA el sol o la luna (unitaria)
                uniform vec3 uSolColor;  // color e intensidad del sol o la luna

                uniform int uNumFarolas;      // cuantas farolas estan encendidas
                uniform vec3 uFarolas[16];    // posicion de cada bombilla
                uniform vec3 uFarolaColor;    // color de la luz de las farolas
                uniform float uFarolaAlcance; // distancia maxima que alcanza cada farola

                uniform int uFaros;        // 1 = faros del auto encendidos
                uniform vec3 uFaroPos[2];  // posicion de los dos focos delanteros
                uniform vec3 uFaroDir;     // hacia donde apuntan (frente del auto)
                uniform float uFaroAlcance;
                uniform float uFaroIntensidad; // de dia el haz casi no se nota

                uniform vec3 uCamara;      // posicion de la camara (para la niebla)
                uniform vec3 uNiebla;      // color del cielo
                uniform float uNieblaInicio;
                uniform float uNieblaFin;

                out vec4 color;

                vec3 conNiebla(vec3 c) {
                    float d = length(vMundo - uCamara);
                    float f = smoothstep(uNieblaInicio, uNieblaFin, d);
                    return mix(c, uNiebla, f);
                }

                void main() {
                    if (uEmision == 1) {
                        color = vec4(conNiebla(uColor), 1.0);
                        return;
                    }

                    vec3 n = normalize(vNormal);

                    // 1) Ambiente + luz direccional (Lambert)
                    vec3 luz = uAmbiente + uSolColor * max(dot(n, uSolDir), 0.0);

                    // 2) Farolas: luz puntual, difusa segun la normal y atenuada con la distancia
                    for (int i = 0; i < uNumFarolas; i++) {
                        vec3 haciaLuz = uFarolas[i] - vMundo;
                        float d = length(haciaLuz);
                        float difusa = max(dot(n, haciaLuz / d), 0.0);
                        float atenuacion = 1.0 / (1.0 + 0.09 * d + 0.016 * d * d);
                        float alcance = 1.0 - smoothstep(uFarolaAlcance * 0.6, uFarolaAlcance, d);
                        luz += uFarolaColor * difusa * atenuacion * alcance * 4.2;
                    }

                    // 3) Faros: dos focos que siguen la posicion y orientacion del auto
                    if (uFaros == 1) {
                        for (int i = 0; i < 2; i++) {
                            vec3 haciaSup = vMundo - uFaroPos[i];
                            float d = length(haciaSup);
                            vec3 dirSup = haciaSup / d;
                            // Cono: 1 en el centro del haz (14 grados), 0 fuera de unos 28 grados
                            float cono = smoothstep(0.88, 0.97, dot(dirSup, uFaroDir));
                            float difusa = max(dot(n, -dirSup), 0.0);
                            float atenuacion = 1.0 / (1.0 + 0.004 * d * d);
                            float alcance = 1.0 - smoothstep(uFaroAlcance * 0.65, uFaroAlcance, d);
                            luz += vec3(1.0, 0.95, 0.78) * cono * difusa * atenuacion * alcance * uFaroIntensidad;
                        }
                    }

                    color = vec4(conNiebla(uColor * luz), 1.0);
                }
                """;
    }

    private int compilar(int tipo, String fuente, String nombre) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(nombre + " shader: " + error);
        }
        return shader;
    }

    private void crearPrograma() {
        int vertices = compilar(GL_VERTEX_SHADER, vertexShader(), "Vertex");
        int fragmentos = compilar(GL_FRAGMENT_SHADER, fragmentShader(), "Fragment");
        programa = glCreateProgram();
        glAttachShader(programa, vertices);
        glAttachShader(programa, fragmentos);
        glLinkProgram(programa);
        glDeleteShader(vertices);
        glDeleteShader(fragmentos);
        if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Error al enlazar programa: " + glGetProgramInfoLog(programa));
        }
    }

    /** Cubo de lado 1 centrado en el origen: 36 vertices con posicion y normal. */
    private void crearCubo() {
        float[] v = {
                // Cara frontal (+Z)
                -0.5f, -0.5f, 0.5f, 0, 0, 1, 0.5f, -0.5f, 0.5f, 0, 0, 1, 0.5f, 0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1, -0.5f, 0.5f, 0.5f, 0, 0, 1, -0.5f, -0.5f, 0.5f, 0, 0, 1,
                // Cara trasera (-Z)
                -0.5f, -0.5f, -0.5f, 0, 0, -1, -0.5f, 0.5f, -0.5f, 0, 0, -1, 0.5f, 0.5f, -0.5f, 0, 0, -1,
                0.5f, 0.5f, -0.5f, 0, 0, -1, 0.5f, -0.5f, -0.5f, 0, 0, -1, -0.5f, -0.5f, -0.5f, 0, 0, -1,
                // Cara izquierda (-X)
                -0.5f, 0.5f, 0.5f, -1, 0, 0, -0.5f, 0.5f, -0.5f, -1, 0, 0, -0.5f, -0.5f, -0.5f, -1, 0, 0,
                -0.5f, -0.5f, -0.5f, -1, 0, 0, -0.5f, -0.5f, 0.5f, -1, 0, 0, -0.5f, 0.5f, 0.5f, -1, 0, 0,
                // Cara derecha (+X)
                0.5f, 0.5f, 0.5f, 1, 0, 0, 0.5f, -0.5f, 0.5f, 1, 0, 0, 0.5f, -0.5f, -0.5f, 1, 0, 0,
                0.5f, -0.5f, -0.5f, 1, 0, 0, 0.5f, 0.5f, -0.5f, 1, 0, 0, 0.5f, 0.5f, 0.5f, 1, 0, 0,
                // Cara superior (+Y)
                -0.5f, 0.5f, -0.5f, 0, 1, 0, -0.5f, 0.5f, 0.5f, 0, 1, 0, 0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, 0.5f, 0, 1, 0, 0.5f, 0.5f, -0.5f, 0, 1, 0, -0.5f, 0.5f, -0.5f, 0, 1, 0,
                // Cara inferior (-Y)
                -0.5f, -0.5f, -0.5f, 0, -1, 0, 0.5f, -0.5f, -0.5f, 0, -1, 0, 0.5f, -0.5f, 0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0, -0.5f, -0.5f, 0.5f, 0, -1, 0, -0.5f, -0.5f, -0.5f, 0, -1, 0
        };
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, v, GL_STATIC_DRAW);
        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
}
