package openvr

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import java.nio.ByteBuffer
import openvr.VR_Init
import org.junit.Test

/**
 * Created by GBarbieri on 25.10.2016.
 */


class Test {

    @Test fun IVRSystem() {

        loadNatives()
        val error = EVRInitError_ByReference()
        val hmd = VR_Init(error, EVRApplicationType.VRApplication_Scene)!!


        val w = IntByReference(0)
        val h = IntByReference(0)
        hmd.getRecommendedRenderTargetSize(w, h)
        assert(w.value > 0 && h.value > 0)


        var m4 = hmd.getProjectionMatrix(EVREye.Eye_Left, .1f, 10f, EGraphicsAPIConvention.API_OpenGL)
        /** 0.75787073  0           -0.05657852   0
         *  0           0.6820195   -0.0013340205 0
         *  0           0           -1.0101011    -0.10101011
         *  0           0           -1            0             */
        assert(m4[0] != 0f && m4[1] == 0f && m4[2] != 0f && m4[3] == 0f
                && m4[4] == 0f && m4[5] != 0f && m4[6] != 0f && m4[7] == 0f
                && m4[8] == 0f && m4[9] == 0f && m4[10] != 0f && m4[11] != 0f
                && m4[12] == 0f && m4[13] == 0f && m4[14] != 0f && m4[15] == 0f)


        val left = FloatByReference()
        val right = FloatByReference()
        val top = FloatByReference()
        val bottom = FloatByReference()
        hmd.getProjectionRaw(EVREye.Eye_Left, left, right, top, bottom)
//      -1.3941408, 1.2448317, -1.4681898, 1.4642779
        assert(left.value < 0 && right.value > 0 && top.value < 0 && bottom.value >= 0)


        val dc = hmd.computeDistortion(EVREye.Eye_Left, .5f, .5f)
        //
        assert(dc.rfRed[0] in 0..1 && dc.rfRed[1] in 0..1 && dc.rfGreen[0] in 0..1 && dc.rfGreen[1] in 0..1 && dc.rfBlue[0] in 0..1 && dc.rfBlue[1] in 0..1)


        val m43 = hmd.getEyeToHeadTransform(EVREye.Eye_Left)
        /** 1   0   0   -0.03045
         *  0   1   0   0
         *  0   0   1   0.015         */
        assert(m43[0] == 1f && m43[1] == 0f && m43[2] == 0f && m43[3] < 0
                && m43[4] == 0f && m43[5] == 1f && m43[6] == 0f && m43[7] == 0f
                && m43[8] == 0f && m43[9] == 0f && m43[10] == 1f && m43[11] > 0)
        println()
//    println("IsDisplayOnDesktop " + IVRSystem.IsDisplayOnDesktop())
//    040 78880
    }
}