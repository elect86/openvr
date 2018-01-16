package openvr.lib

import com.sun.jna.Callback
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.ByteByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.util.*

// ivrtrackedcamera.h =============================================================================================================================================

open class IVRTrackedCamera : Structure {

    /** Returns a string for an error */
    fun getCameraErrorNameFromEnum(eCameraError: EVRTrackedCameraError) = GetCameraErrorNameFromEnum!!.invoke(eCameraError.i)

    @JvmField var GetCameraErrorNameFromEnum: GetCameraErrorNameFromEnum_callback? = null

    interface GetCameraErrorNameFromEnum_callback : Callback {
        fun invoke(eCameraError: Int): String
    }

    /** For convenience, same as tracked property request Prop_HasCamera_Bool */
    // TODO check automatic conversion *Boolean -> *Byte
    fun hasCamera(nDeviceIndex: TrackedDeviceIndex, pHasCamera: BooleanByReference) = EVRTrackedCameraError.of(HasCamera!!.invoke(nDeviceIndex, pHasCamera))

    @JvmField var HasCamera: HasCamera_callback? = null

    interface HasCamera_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, pHasCamera: ByteByReference): Int
    }

    /** Gets size of the image frame. */
    fun getCameraFrameSize(nDeviceIndex: TrackedDeviceIndex, eFrameType: EVRTrackedCameraFrameType, pnWidth: IntByReference, pnHeight: IntByReference,
                           pnFrameBufferSize: IntByReference)
            = EVRTrackedCameraError.of(GetCameraFrameSize!!.invoke(nDeviceIndex, eFrameType.i, pnWidth, pnHeight, pnFrameBufferSize))

    @JvmField var GetCameraFrameSize: GetCameraFrameSize_callback? = null

    interface GetCameraFrameSize_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, eFrameType: Int, pnWidth: IntByReference, pnHeight: IntByReference, pnFrameBufferSize: IntByReference): Int
    }


    fun GetCameraIntrinsics(nDeviceIndex: TrackedDeviceIndex, eFrameType: EVRTrackedCameraFrameType, pFocalLength: HmdVec2.ByReference,
                            pCenter: HmdVec2.ByReference)
            = EVRTrackedCameraError.of(GetCameraIntrinsics!!.invoke(nDeviceIndex, eFrameType.i, pFocalLength, pCenter))

    @JvmField var GetCameraIntrinsics: GetCameraIntrinsics_callback? = null

    interface GetCameraIntrinsics_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, eFrameType: Int, pFocalLength: HmdVec2.ByReference, pCenter: HmdVec2.ByReference): Int
    }


    fun getCameraProjection(nDeviceIndex: TrackedDeviceIndex, eFrameType: EVRTrackedCameraFrameType, flZNear: Float, flZFar: Float,
                            pProjection: HmdMat44.ByReference)
            = EVRTrackedCameraError.of(GetCameraProjection!!.invoke(nDeviceIndex, eFrameType.i, flZNear, flZFar, pProjection))

    @JvmField var GetCameraProjection: GetCameraProjection_callback? = null

    interface GetCameraProjection_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, eFrameType: Int, flZNear: Float, flZFar: Float, pProjection: HmdMat44.ByReference): Int
    }

    /** Acquiring streaming service permits video streaming for the caller. Releasing hints the system that video services do not need to be maintained for this
     *  client.
     *  If the camera has not already been activated, a one time spin up may incur some auto exposure as well as initial streaming frame delays.
     *  The camera should be considered a global resource accessible for shared consumption but not exclusive to any caller.
     *  The camera may go inactive due to lack of active consumers or headset idleness. */
    fun acquireVideoStreamingService(nDeviceIndex: TrackedDeviceIndex, pHandle: TrackedCameraHandle)
            = EVRTrackedCameraError.of(AcquireVideoStreamingService!!.invoke(nDeviceIndex, pHandle))

    @JvmField var AcquireVideoStreamingService: AcquireVideoStreamingService_callback? = null

    interface AcquireVideoStreamingService_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, pHandle: TrackedCameraHandle): Int
    }

    fun releaseVideoStreamingService(hTrackedCamera: TrackedCameraHandle) = EVRTrackedCameraError.of(ReleaseVideoStreamingService!!.invoke(hTrackedCamera))
    @JvmField var ReleaseVideoStreamingService: ReleaseVideoStreamingService_callback? = null

    interface ReleaseVideoStreamingService_callback : Callback {
        fun invoke(hTrackedCamera: TrackedCameraHandle): Int
    }

    /** Copies the image frame into a caller's provided buffer. The image data is currently provided as RGBA data, 4 bytes per pixel.
     *  A caller can provide null for the framebuffer or frameheader if not desired. Requesting the frame header first, followed by the frame buffer allows
     *  the caller to determine if the frame as advanced per the frame header sequence.
     *  If there is no frame available yet, due to initial camera spinup or re-activation, the error will be VRTrackedCameraError_NoFrameAvailable.
     *  Ideally a caller should be polling at ~16ms intervals */
    fun getVideoStreamFrameBuffer(hTrackedCamera: TrackedCameraHandle, eFrameType: EVRTrackedCameraFrameType, pFrameBuffer: Pointer, nFrameBufferSize: Int,
                                  pFrameHeader: CameraVideoStreamFrameHeader.ByReference, nFrameHeaderSize: Int)
            = EVRTrackedCameraError.of(GetVideoStreamFrameBuffer!!.invoke(hTrackedCamera, eFrameType.i, pFrameBuffer, nFrameBufferSize, pFrameHeader,
            nFrameHeaderSize))

    @JvmField var GetVideoStreamFrameBuffer: GetVideoStreamFrameBuffer_callback? = null

    interface GetVideoStreamFrameBuffer_callback : Callback {
        fun invoke(hTrackedCamera: TrackedCameraHandle, eFrameType: Int, pFrameBuffer: Pointer, nFrameBufferSize: Int,
                   pFrameHeader: CameraVideoStreamFrameHeader.ByReference, nFrameHeaderSize: Int): Int
    }

    /** Gets size of the image frame. */
    fun getVideoStreamTextureSize(nDeviceIndex: TrackedDeviceIndex, eFrameType: EVRTrackedCameraFrameType, pTextureBounds: VRTextureBounds.ByReference,
                                  pnWidth: IntByReference, pnHeight: IntByReference)
            = EVRTrackedCameraError.of(GetVideoStreamTextureSize!!.invoke(nDeviceIndex, eFrameType.i, pTextureBounds, pnWidth, pnHeight))

    @JvmField var GetVideoStreamTextureSize: GetVideoStreamTextureSize_callback? = null

    interface GetVideoStreamTextureSize_callback : Callback {
        fun invoke(nDeviceIndex: TrackedDeviceIndex, eFrameType: Int, pTextureBounds: VRTextureBounds.ByReference, pnWidth: IntByReference,
                   pnHeight: IntByReference): Int
    }

    /** Access a shared D3D11 texture for the specified tracked camera stream.
     *  The camera frame type VRTrackedCameraFrameType_Undistorted is not supported directly as a shared texture. It is an interior
     *  subregion of the shared texture VRTrackedCameraFrameType_MaximumUndistorted.
     *  Instead, use GetVideoStreamTextureSize() with VRTrackedCameraFrameType_Undistorted to determine the proper interior subregion
     *  bounds along with GetVideoStreamTextureD3D11() with VRTrackedCameraFrameType_MaximumUndistorted to provide the texture.
     *  The VRTrackedCameraFrameType_MaximumUndistorted will yield an image where the invalid regions are decoded by the alpha channel
     *  having a zero component. The valid regions all have a non-zero alpha component. The subregion as described by
     *  VRTrackedCameraFrameType_Undistorted guarantees a rectangle where all pixels are valid. */
    fun getVideoStreamTextureD3D11(hTrackedCamera: TrackedCameraHandle, eFrameType: EVRTrackedCameraFrameType, pD3D11DeviceOrResource: Pointer,
                                   ppD3D11ShaderResourceView: PointerByReference, pFrameHeader: CameraVideoStreamFrameHeader.ByReference, nFrameHeaderSize: Int)
            = EVRTrackedCameraError.of(GetVideoStreamTextureD3D11!!.invoke(hTrackedCamera, eFrameType.i, pD3D11DeviceOrResource, ppD3D11ShaderResourceView,
            pFrameHeader, nFrameHeaderSize))

    @JvmField var GetVideoStreamTextureD3D11: GetVideoStreamTextureD3D11_callback? = null

    interface GetVideoStreamTextureD3D11_callback : Callback {
        fun invoke(hTrackedCamera: TrackedCameraHandle, eFrameType: Int, pD3D11DeviceOrResource: Pointer, ppD3D11ShaderResourceView: PointerByReference,
                   pFrameHeader: CameraVideoStreamFrameHeader.ByReference, nFrameHeaderSize: Int): Int
    }

    /** Access a shared GL texture for the specified tracked camera stream */
    fun getVideoStreamTextureGL(hTrackedCamera: TrackedCameraHandle, eFrameType: EVRTrackedCameraFrameType, pglTextureId: glUInt_ByReference,
                                pFrameHeader: CameraVideoStreamFrameHeader.ByReference, nFrameHeaderSize: Int)
            = EVRTrackedCameraError.of(GetVideoStreamTextureGL!!.invoke(hTrackedCamera, eFrameType.i, pglTextureId, pFrameHeader, nFrameHeaderSize))

    @JvmField var GetVideoStreamTextureGL: GetVideoStreamTextureGL_callback? = null

    interface GetVideoStreamTextureGL_callback : Callback {
        fun invoke(hTrackedCamera: TrackedCameraHandle, eFrameType: Int, pglTextureId: IntByReference, pFrameHeader: CameraVideoStreamFrameHeader.ByReference,
                   nFrameHeaderSize: Int): Int
    }


    fun releaseVideoStreamTextureGL(hTrackedCamera: TrackedCameraHandle, glTextureId: glUInt)
            = EVRTrackedCameraError.of(ReleaseVideoStreamTextureGL!!.invoke(hTrackedCamera, glTextureId))

    @JvmField var ReleaseVideoStreamTextureGL: ReleaseVideoStreamTextureGL_callback? = null

    interface ReleaseVideoStreamTextureGL_callback : Callback {
        fun invoke(hTrackedCamera: TrackedCameraHandle, glTextureId: Int): Int
    }

    constructor()

    override fun getFieldOrder(): List<String> = Arrays.asList("GetCameraErrorNameFromEnum", "HasCamera", "GetCameraFrameSize",
            "GetCameraIntrinsics", "GetCameraProjection", "AcquireVideoStreamingService", "ReleaseVideoStreamingService",
            "GetVideoStreamFrameBuffer", "GetVideoStreamTextureSize", "GetVideoStreamTextureD3D11", "GetVideoStreamTextureGL",
            "ReleaseVideoStreamTextureGL")

    constructor(peer: Pointer) : super(peer) {
        read()
    }

    class ByReference : IVRTrackedCamera(), Structure.ByReference
    class ByValue : IVRTrackedCamera(), Structure.ByValue
}

val IVRTrackedCamera_Version = "IVRTrackedCamera_003"