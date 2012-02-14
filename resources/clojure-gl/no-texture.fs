varying vec4 color;
varying vec4 vertNormal;

void main(void) {
 float shade = abs(dot(normalize(vertNormal.xyz), vec3(1.0, 0.0, 0.0)));
 gl_FragColor = vec4(color.rgb * shade, 1.0);
}
