package net.infdev.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class TextRenderer {

    private static class Character {
        int textureID;
        int width;
        int height;
        int bearingX;
        int bearingY;
        int advance;
    }

    private final Map<Integer, Character> characters = new HashMap<>();
    private int vao, vbo;
    private int shaderProgram;

    public void init(String fontPath, int fontSize) throws IOException {
        ByteBuffer fontBuffer = ioResourceToByteBuffer(fontPath, 512 * 1024);

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new IllegalStateException("Failed to initialize font");
        }

        try (MemoryStack stack = stackPush()) {
            float scale = stbtt_ScaleForPixelHeight(fontInfo, fontSize);

            for (int c = 32; c < 128; c++) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                IntBuffer pXOff = stack.mallocInt(1);
                IntBuffer pYOff = stack.mallocInt(1);

                ByteBuffer bitmap = stbtt_GetCodepointBitmap(fontInfo, 0, scale, c, pWidth, pHeight, pXOff, pYOff);

                Character character = new Character();

                if (bitmap != null && pWidth.get(0) > 0 && pHeight.get(0) > 0) {
                    int textureID = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, textureID);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RED,
                            pWidth.get(0), pHeight.get(0),
                            0, GL_RED, GL_UNSIGNED_BYTE, bitmap);

                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

                    character.textureID = textureID;
                    character.width = pWidth.get(0);
                    character.height = pHeight.get(0);
                    character.bearingX = pXOff.get(0);
                    character.bearingY = pYOff.get(0);

                    stbtt_FreeBitmap(bitmap, MemoryUtil.NULL);
                } else {
                    character.textureID = 0;
                    character.width = 0;
                    character.height = 0;
                    character.bearingX = 0;
                    character.bearingY = 0;
                }

                IntBuffer pAdvanceWidth = stack.mallocInt(1);
                IntBuffer pLeftBearing = stack.mallocInt(1);
                stbtt_GetCodepointHMetrics(fontInfo, c, pAdvanceWidth, pLeftBearing);

                character.advance = (int) (pAdvanceWidth.get(0) * scale);

                characters.put(c, character);
            }
        }

        // Setup VAO/VBO for text rendering
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL30.glBindVertexArray(0);

        // Build shaders
        shaderProgram = createTextShader();

        // Bind sampler to texture unit 0
        int samplerLoc = glGetUniformLocation(shaderProgram, "text");
        glUseProgram(shaderProgram);
        glUniform1i(samplerLoc, 0);
        glUseProgram(0);
    }

    public void renderText(String text, float x, float y, float scale,
                           float r, float g, float b, float[] projection) {
        glUseProgram(shaderProgram);

        int projLoc = glGetUniformLocation(shaderProgram, "projection");
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        fb.put(projection).flip();
        glUniformMatrix4fv(projLoc, false, fb);

        int colorLoc = glGetUniformLocation(shaderProgram, "textColor");
        glUniform3f(colorLoc, r, g, b);

        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vao);

        for (int i = 0; i < text.length(); i++) {
            Character ch = characters.get((int) text.charAt(i));
            if (ch == null) continue;

            float xpos = x + ch.bearingX * scale;
            float ypos = y - (ch.height - ch.bearingY) * scale;

            float w = ch.width * scale;
            float h = ch.height * scale;

            float[] vertices = {
                    xpos,     ypos + h, 0.0f, 0.0f,
                    xpos,     ypos,     0.0f, 1.0f,
                    xpos + w, ypos,     1.0f, 1.0f,

                    xpos,     ypos + h, 0.0f, 0.0f,
                    xpos + w, ypos,     1.0f, 1.0f,
                    xpos + w, ypos + h, 1.0f, 0.0f
            };

            glBindTexture(GL_TEXTURE_2D, ch.textureID);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            glDrawArrays(GL_TRIANGLES, 0, 6);

            x += ch.advance;
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glUseProgram(0);
    }

    private int createTextShader() {
        String vs =
                "#version 330 core\n" +
                "layout (location = 0) in vec4 vertex;\n" +
                "out vec2 TexCoords;\n" +
                "uniform mat4 projection;\n" +
                "void main() {\n" +
                "    gl_Position = projection * vec4(vertex.xy, 0.0, 1.0);\n" +
                "    TexCoords = vertex.zw;\n" +
                "}";

        String fs =
                "#version 330 core\n" +
                "in vec2 TexCoords;\n" +
                "out vec4 FragColor;\n" +
                "uniform sampler2D text;\n" +
                "uniform vec3 textColor;\n" +
                "void main() {\n" +
                "    float alpha = texture(text, TexCoords).r;\n" +
                "    FragColor = vec4(textColor, alpha);\n" +
                "}";

        int vShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vShader, vs);
        glCompileShader(vShader);
        if (glGetShaderi(vShader, GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Text vertex shader error: " + glGetShaderInfoLog(vShader));

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fShader, fs);
        glCompileShader(fShader);
        if (glGetShaderi(fShader, GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Text fragment shader error: " + glGetShaderInfoLog(fShader));

        int program = glCreateProgram();
        glAttachShader(program, vShader);
        glAttachShader(program, fShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Text shader linking failed: " + glGetProgramInfoLog(program));

        glDeleteShader(vShader);
        glDeleteShader(fShader);

        return program;
    }

    private ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        try (InputStream source = getClass().getResourceAsStream(resource)) {
            if (source == null) throw new IOException("Resource not found: " + resource);
            byte[] bytes = source.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        }
    }
}