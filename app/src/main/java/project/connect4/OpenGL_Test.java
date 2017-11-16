package project.connect4;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class OpenGL_Test extends AppCompatActivity {

    private GLSurfaceView mGLViewl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLViewl = new MyGLSurfaceView(this);

        setContentView(mGLViewl);
    }
}
