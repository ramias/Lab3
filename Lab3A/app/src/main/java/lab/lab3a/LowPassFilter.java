package lab.lab3a;

/**
 * Created by Thomas on 2015-12-08.
 */
public class LowPassFilter {

    private static final float ALPHA = 0.15f;


    protected static float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
}
