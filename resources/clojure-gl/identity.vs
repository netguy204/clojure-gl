attribute vec4 vVertex;
attribute vec2 vTexCoord0;
uniform mat4 mvMatrix;
varying vec2 vTex;

void main(void) {
 vTex = vTexCoord0;
 gl_Position = mvMatrix * vVertex;
}

