package openvr

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import kool.adr
import openvr.lib.*
import openvr.lib.VREventType as ET
import openvr.lib.vr.maxTrackedDeviceCount
import openvr.plugin.Render
import openvr.plugin.Utils
import org.lwjgl.openvr.VRControllerState
import org.lwjgl.openvr.VREvent
import org.lwjgl.openvr.VREventController

open class EventListener {

    val hmd = vrSystem

    val states = Array(vr.maxTrackedDeviceCount) { VRControllerState.calloc() }

    var left = -1
    var right = -1

    val devices = Array(maxTrackedDeviceCount, ::Device)

    init {
        updateRoles()
    }

    private fun updateRoles() {
        left = -1
        right = -1
        for (i in 0 until maxTrackedDeviceCount) {
            val dc: TrackedDeviceClass = hmd.getTrackedDeviceClass(i)
            val cr: TrackedControllerRole = hmd.getControllerRoleForTrackedDeviceIndex(i)

            if (dc == TrackedDeviceClass.Controller && hmd.getControllerState(i, states[i])) {
                if (cr == TrackedControllerRole.LeftHand)
                    left = i
                else if (cr == TrackedControllerRole.RightHand)
                    right = i
            }
        }
        devices.getOrNull(left)?.left = true
        devices.getOrNull(right)?.left = false
        println("new roles, left: $left, right: $right")
    }

    val event = VREvent.calloc()
    var frameCount = 0

    fun poll() {

        while (hmd.pollNextEvent(event, event.sizeof())) process()

        devices.getOrNull(left)?.update(frameCount)
        devices.getOrNull(right)?.update(frameCount)

        frameCount++
    }

    private fun process() {

        when (event.eventType()) {
            ET.TrackedDeviceActivated.i -> updateRoles().also { trackedDeviceActivated(event.trackedDeviceIndex == left) }
            ET.TrackedDeviceDeactivated.i -> updateRoles().also { trackedDeviceDeactivated(event.trackedDeviceIndex == left) }
            ET.TrackedDeviceUpdated.i -> updateRoles().also { trackedDeviceUpdated(event.trackedDeviceIndex == left) }
            ET.TrackedDeviceUserInteractionStarted.i -> updateRoles().also { trackedDeviceUserInteractionStarted(event.trackedDeviceIndex == left) }
            ET.TrackedDeviceUserInteractionEnded.i -> updateRoles().also { trackedDeviceUserInteractionEnded(event.trackedDeviceIndex == left) }
            ET.IpdChanged.i -> ipdChanged()
            ET.EnterStandbyMode.i -> enterStandbyMode()
            ET.LeaveStandbyMode.i -> leaveStandbyMode()
            ET.TrackedDeviceRoleChanged.i -> updateRoles().also { trackedDeviceRoleChanged(event.trackedDeviceIndex == left) }
            ET.WatchdogWakeUpRequested.i -> watchdogWakeUpRequested()
            ET.LensDistortionChanged.i -> lensDistortionChanged()
            ET.PropertyChanged.i -> propertyChanged()
            ET.WirelessDisconnect.i -> wirelessDisconnect()
            ET.WirelessReconnect.i -> wirelessReconnect()
            ET.ButtonPress.i -> buttonPress(event.trackedDeviceIndex == left, VREventController.create(event.data.adr).button)
            ET.ButtonUnpress.i -> buttonUnpress(event.trackedDeviceIndex == left, VREventController.create(event.data.adr).button)
            ET.ButtonTouch.i -> buttonTouch(event.trackedDeviceIndex == left, VREventController.create(event.data.adr).button)
            ET.ButtonUntouch.i -> buttonUntouch(event.trackedDeviceIndex == left, VREventController.create(event.data.adr).button)
            ET.MouseMove.i -> mouseMove()
            ET.MouseButtonDown.i -> mouseButtonDown()
            ET.MouseButtonUp.i -> mouseButtonUp()
            ET.FocusEnter.i -> focusEnter()
            ET.FocusLeave.i -> focusLeave()
            ET.ScrollDiscrete.i -> scroll()
            ET.TouchPadMove.i -> {
                val state = vr.VRControllerState()
                val pos = when {
                    hmd.getControllerState(event.trackedDeviceIndex, state) -> Vec2()
                    else -> Vec2(state.axis[0].pos)
                }
                touchpadMove(event.trackedDeviceIndex == left, pos)
            }
            ET.OverlayFocusChanged.i -> overlayFocusChanged()
            // VREventType.TriggerMove JVM specific
            ET.InputFocusCaptured.i -> inputFocusCaptured()
            ET.InputFocusReleased.i -> inputFocusReleased()
            ET.SceneFocusLost.i -> sceneFocusLost()
            ET.SceneFocusGained.i -> sceneFocusGained()
            ET.SceneApplicationChanged.i -> sceneApplicationChanged()
            ET.SceneFocusChanged.i -> sceneFocusChanged()
            ET.InputFocusChanged.i -> inputFocusChanged()
            ET.SceneApplicationSecondaryRenderingStarted.i -> sceneApplicationSecondaryRenderingStarted()
            ET.HideRenderModels.i -> hideRenderModels()
            ET.ShowRenderModels.i -> showRenderModels()
            ET.OverlayShown.i -> overlayShown()
            ET.OverlayHidden.i -> overlayHidden()
            ET.DashboardActivated.i -> dashboardActivated()
            ET.DashboardDeactivated.i -> dashboardDeactivated()
            //ET.DashboardThumbSelected.i -> dashboardThumbSelected()
            ET.DashboardRequested.i -> dashboardRequested()
            ET.ResetDashboard.i -> resetDashboard()
            ET.RenderToast.i -> renderToast()
            ET.ImageLoaded.i -> imageLoaded()
            ET.ShowKeyboard.i -> showKeyboard()
            ET.HideKeyboard.i -> hideKeyboard()
            ET.OverlayGamepadFocusGained.i -> overlayGamepadFocusGained()
            ET.OverlayGamepadFocusLost.i -> overlayGamepadFocusLost()
//            VREventType.OverlaySharedTextureChanged -> overlaySharedTextureChanged()
//            VREventType.DashboardGuideButtonDown -> dashboardGuideButtonDown()
            ET.DashboardGuideButtonUp.i -> dashboardGuideButtonUp()
            ET.ScreenshotTriggered.i -> screenshotTriggered()
            ET.ImageFailed.i -> imageFailed()
            ET.DashboardOverlayCreated.i -> dashboardOverlayCreated()
            ET.RequestScreenshot.i -> requestScreenshot()
            ET.ScreenshotTaken.i -> screenshotTaken()
            ET.ScreenshotFailed.i -> screenshotFailed()
            ET.SubmitScreenshotToDashboard.i -> submitScreenshotToDashboard()
            ET.ScreenshotProgressToDashboard.i -> screenshotProgressToDashboard()
            ET.PrimaryDashboardDeviceChanged.i -> primaryDashboardDeviceChanged()
            ET.Notification_Shown.i -> notification_Shown()
            ET.Notification_Hidden.i -> notification_Hidden()
            ET.Notification_BeginInteraction.i -> notification_BeginInteraction()
            ET.Notification_Destroyed.i -> notification_Destroyed()
            ET.Quit.i -> quit()
            ET.ProcessQuit.i -> processQuit()
            ET.QuitAborted_UserPrompt.i -> quitAborted_UserPrompt()
            ET.QuitAcknowledged.i -> quitAcknowledged()
            ET.DriverRequestedQuit.i -> driverRequestedQuit()
            ET.ChaperoneDataHasChanged.i -> chaperoneDataHasChanged()
            ET.ChaperoneUniverseHasChanged.i -> chaperoneUniverseHasChanged()
            ET.ChaperoneTempDataHasChanged.i -> chaperoneTempDataHasChanged()
            ET.ChaperoneSettingsHaveChanged.i -> chaperoneSettingsHaveChanged()
            ET.SeatedZeroPoseReset.i -> seatedZeroPoseReset()
            ET.AudioSettingsHaveChanged.i -> audioSettingsHaveChanged()
            ET.BackgroundSettingHasChanged.i -> backgroundSettingHasChanged()
            ET.CameraSettingsHaveChanged.i -> cameraSettingsHaveChanged()
            ET.ReprojectionSettingHasChanged.i -> reprojectionSettingHasChanged()
            ET.ModelSkinSettingsHaveChanged.i -> modelSkinSettingsHaveChanged()
            ET.EnvironmentSettingsHaveChanged.i -> environmentSettingsHaveChanged()
            ET.PowerSettingsHaveChanged.i -> powerSettingsHaveChanged()
            ET.EnableHomeAppSettingsHaveChanged.i -> enableHomeAppSettingsHaveChanged()
            ET.StatusUpdate.i -> statusUpdate()
            ET.MCImageUpdated.i -> mcImageUpdated()
            ET.FirmwareUpdateStarted.i -> firmwareUpdateStarted()
            ET.FirmwareUpdateFinished.i -> firmwareUpdateFinished()
            ET.KeyboardClosed.i -> keyboardClosed()
            ET.KeyboardCharInput.i -> keyboardCharInput()
            ET.KeyboardDone.i -> keyboardDone()
            ET.ApplicationTransitionStarted.i -> applicationTransitionStarted()
            ET.ApplicationTransitionAborted.i -> applicationTransitionAborted()
            ET.ApplicationTransitionNewAppStarted.i -> applicationTransitionNewAppStarted()
            ET.ApplicationListUpdated.i -> applicationListUpdated()
            ET.ApplicationMimeTypeLoad.i -> applicationMimeTypeLoad()
            ET.ApplicationTransitionNewAppLaunchComplete.i -> applicationTransitionNewAppLaunchComplete()
            ET.ProcessConnected.i -> processConnected()
            ET.ProcessDisconnected.i -> processDisconnected()
            ET.Compositor_MirrorWindowShown.i -> compositor_MirrorWindowShown()
            ET.Compositor_MirrorWindowHidden.i -> compositor_MirrorWindowHidden()
            ET.Compositor_ChaperoneBoundsShown.i -> compositor_ChaperoneBoundsShown()
            ET.Compositor_ChaperoneBoundsHidden.i -> compositor_ChaperoneBoundsHidden()
            ET.TrackedCamera_StartVideoStream.i -> trackedCamera_StartVideoStream()
            ET.TrackedCamera_StopVideoStream.i -> trackedCamera_StopVideoStream()
            ET.TrackedCamera_PauseVideoStream.i -> trackedCamera_PauseVideoStream()
            ET.TrackedCamera_ResumeVideoStream.i -> trackedCamera_ResumeVideoStream()
            ET.TrackedCamera_EditingSurface.i -> trackedCamera_EditingSurface()
            ET.PerformanceTest_EnableCapture.i -> performanceTest_EnableCapture()
            ET.PerformanceTest_DisableCapture.i -> performanceTest_DisableCapture()
            ET.PerformanceTest_FidelityLevel.i -> performanceTest_FidelityLevel()
            ET.MessageOverlay_Closed.i -> messageOverlay_Closed()

            else -> {
                ET.values().find { it.i == event.eventType() }?.let { e ->
                    println("WARNING:Event type ${e.name} (${e.i}) is not handled")
                } ?: println("WARNING:Event type(${event.eventType}) is unknown")
            }   // None, VendorSpecific_Reserved_Start / End
        }
    }

    open fun trackedDeviceActivated(left: Boolean) {}
    open fun trackedDeviceDeactivated(left: Boolean) {}
    open fun trackedDeviceUpdated(left: Boolean) {}
    open fun trackedDeviceUserInteractionStarted(left: Boolean) {}
    open fun trackedDeviceUserInteractionEnded(left: Boolean) {}
    open fun ipdChanged() {}
    open fun enterStandbyMode() {}
    open fun leaveStandbyMode() {}
    open fun trackedDeviceRoleChanged(left: Boolean) {}
    open fun watchdogWakeUpRequested() {}
    open fun lensDistortionChanged() {}
    open fun propertyChanged() {}
    open fun wirelessDisconnect() {}
    open fun wirelessReconnect() {}
    open fun buttonPress(left: Boolean, button: VRButtonId) {}
    open fun buttonUnpress(left: Boolean, button: VRButtonId) {}
    open fun buttonTouch(left: Boolean, button: VRButtonId) {}
    open fun buttonUntouch(left: Boolean, button: VRButtonId) {}
    open fun mouseMove() {}
    open fun mouseButtonDown() {}
    open fun mouseButtonUp() {}
    open fun focusEnter() {}
    open fun focusLeave() {}
    open fun scroll() {}
    open fun touchpadMove(left: Boolean, pos: Vec2) {}
    open fun overlayFocusChanged() {}
    open fun triggerMove(left: Boolean, state: Boolean, limit: Float, value: Float) {}
    open fun inputFocusCaptured() {}
    open fun inputFocusReleased() {}
    open fun sceneFocusLost() {}
    open fun sceneFocusGained() {}
    open fun sceneApplicationChanged() {}
    open fun sceneFocusChanged() {}
    open fun inputFocusChanged() {}
    open fun sceneApplicationSecondaryRenderingStarted() {}
    open fun hideRenderModels() {}
    open fun showRenderModels() {}
    open fun overlayShown() {}
    open fun overlayHidden() {}
    open fun dashboardActivated() {}
    open fun dashboardDeactivated() {}
    open fun dashboardThumbSelected() {}
    open fun dashboardRequested() {}
    open fun resetDashboard() {}
    open fun renderToast() {}
    open fun imageLoaded() {}
    open fun showKeyboard() {}
    open fun hideKeyboard() {}
    open fun overlayGamepadFocusGained() {}
    open fun overlayGamepadFocusLost() {}
    open fun overlaySharedTextureChanged() {}
    open fun dashboardGuideButtonDown() {}
    open fun dashboardGuideButtonUp() {}
    open fun screenshotTriggered() {}
    open fun imageFailed() {}
    open fun dashboardOverlayCreated() {}
    open fun requestScreenshot() {}
    open fun screenshotTaken() {}
    open fun screenshotFailed() {}
    open fun submitScreenshotToDashboard() {}
    open fun screenshotProgressToDashboard() {}
    open fun primaryDashboardDeviceChanged() {}
    open fun notification_Shown() {}
    open fun notification_Hidden() {}
    open fun notification_BeginInteraction() {}
    open fun notification_Destroyed() {}
    open fun quit() {}
    open fun processQuit() {}
    open fun quitAborted_UserPrompt() {}
    open fun quitAcknowledged() {}
    open fun driverRequestedQuit() {}
    open fun chaperoneDataHasChanged() {}
    open fun chaperoneUniverseHasChanged() {}
    open fun chaperoneTempDataHasChanged() {}
    open fun chaperoneSettingsHaveChanged() {}
    open fun seatedZeroPoseReset() {}
    open fun audioSettingsHaveChanged() {}
    open fun backgroundSettingHasChanged() {}
    open fun cameraSettingsHaveChanged() {}
    open fun reprojectionSettingHasChanged() {}
    open fun modelSkinSettingsHaveChanged() {}
    open fun environmentSettingsHaveChanged() {}
    open fun powerSettingsHaveChanged() {}
    open fun enableHomeAppSettingsHaveChanged() {}
    open fun statusUpdate() {}
    open fun mcImageUpdated() {}
    open fun firmwareUpdateStarted() {}
    open fun firmwareUpdateFinished() {}
    open fun keyboardClosed() {}
    open fun keyboardCharInput() {}
    open fun keyboardDone() {}
    open fun applicationTransitionStarted() {}
    open fun applicationTransitionAborted() {}
    open fun applicationTransitionNewAppStarted() {}
    open fun applicationListUpdated() {}
    open fun applicationMimeTypeLoad() {}
    open fun applicationTransitionNewAppLaunchComplete() {}
    open fun processConnected() {}
    open fun processDisconnected() {}
    open fun compositor_MirrorWindowShown() {}
    open fun compositor_MirrorWindowHidden() {}
    open fun compositor_ChaperoneBoundsShown() {}
    open fun compositor_ChaperoneBoundsHidden() {}
    open fun trackedCamera_StartVideoStream() {}
    open fun trackedCamera_StopVideoStream() {}
    open fun trackedCamera_PauseVideoStream() {}
    open fun trackedCamera_ResumeVideoStream() {}
    open fun trackedCamera_EditingSurface() {}
    open fun performanceTest_EnableCapture() {}
    open fun performanceTest_DisableCapture() {}
    open fun performanceTest_FidelityLevel() {}
    open fun messageOverlay_Closed() {}


    val leftDevice get() = devices.getOrNull(left)
    val rightDevice get() = devices.getOrNull(right)


    // Controllers Settings
    var leftTouchpadMode = TouchpadMode.Off
    var leftTouchpadDelta = .1f
    var leftTriggerMode = TriggerMode.OnState
    var leftTriggerDelta = .1f

    var rightTouchpadMode = TouchpadMode.Off
    var rightTouchpadDelta = .1f
    var rightTriggerMode = TriggerMode.Off
    var rightTriggerDelta = .1f

    enum class ButtonMask(val i: Long) {
        /** reserved    */
        System(1L shl VRButtonId.System.i),
        ApplicationMenu(1L shl VRButtonId.ApplicationMenu.i),
        Grip(1L shl VRButtonId.Grip.i),
        Axis0(1L shl VRButtonId.Axis0.i),
        Axis1(1L shl VRButtonId.Axis1.i),
        Axis2(1L shl VRButtonId.Axis2.i),
        Axis3(1L shl VRButtonId.Axis3.i),
        Axis4(1L shl VRButtonId.Axis4.i),
        SteamVR_Touchpad(1L shl VRButtonId.SteamVR_Touchpad.i),
        SteamVR_Trigger(1L shl VRButtonId.SteamVR_Trigger.i);
    }

    enum class TouchpadMode { Off, OnDelta, Always }
    enum class TriggerMode { Off, OnState, OnLimit, Always }

    fun dispose() = devices.forEach { it.dispose() }

    inner class Device(val index: Int) {

        var _frameCount = -1

        var valid = false
            private set

        var left = true

        val connected get() = update().run { _pose.deviceIsConnected }
        val hasTracking get() = update().run { _pose.poseIsValid }
        val outOfRange get() = update().run { _pose.trackingResult == TrackingResult.Running_OutOfRange || _pose.trackingResult == TrackingResult.Calibrating_OutOfRange }
        val calibrating get() = update().run { _pose.trackingResult == TrackingResult.Calibrating_InProgress || _pose.trackingResult == TrackingResult.Calibrating_OutOfRange }
        val uninitialized get() = update().run { _pose.trackingResult == TrackingResult.Uninitialized }

        private val _transform = Utils.RigidTransform()

        /*  These values are only accurate for the last controller state change (e.g. trigger release),
            and by definition, will always lag behind the predicted visual poses that drive SteamVR_TrackedObjects
            since they are sync'd to the input timestamp that caused them to update.    */
        val transform get() = update().run { _transform put _pose.deviceToAbsoluteTracking }
        val velocity get() = update().run { _velocity.put(_pose.velocity.x, _pose.velocity.y, -_pose.velocity.z) }
        val angularVelocity get() = update().run { _angularVelocity.put(-_pose.angularVelocity.x, -_pose.angularVelocity.y, _pose.angularVelocity.z) }
        val state get() = update().run { _state }
        val prevState get() = update().run { _prevState }
        val pose get() = update().run { _pose }

        private val _velocity = Vec3()
        private val _angularVelocity = Vec3()
        private val _state = VRControllerState()
        private val _prevState = VRControllerState()
        private val _pose = TrackedDevicePose()
        private var prevFrameCount = -1
        private val prevTouchpadPos = Vec2()
        val touchpadMode get() = if (left) leftTouchpadMode else rightTouchpadMode
        /** amount touchpad position must be increased or released on a single axes to change state   */
        val touchpadDelta get() = if (left) leftTouchpadDelta else rightTouchpadDelta

        fun update(frameCount: Int = _frameCount) {
            if (frameCount != prevFrameCount) {
                prevFrameCount = frameCount
                valid = hmd.getControllerStateWithPose(Render.trackingSpace, index, _state, _pose)
                if (valid) {
                    updateTouchpad()
                    updateTrigger()
                }
            }
        }

        fun updateTouchpad() {
            if (ButtonMask.SteamVR_Touchpad.touch) when (touchpadMode) {
                TouchpadMode.Off -> Unit
                TouchpadMode.OnDelta -> {
                    if (prevTouchpadPos.x - _state.axis[0].pos.x >= touchpadDelta || prevTouchpadPos.y - _state.axis[0].pos.y >= touchpadDelta) {
                        touchpadMove(left, _state.axis[0].pos)
                        prevTouchpadPos put _state.axis[0].pos
                    }
                }
                TouchpadMode.Always -> {
                    touchpadMove(left, _state.axis[0].pos)
                    prevTouchpadPos put _state.axis[0].pos
                }
            }

        }

        val ButtonMask.press get() = _state.buttonPressed has this
        val ButtonMask.pressDown get() = _state.buttonPressed has this && _prevState.buttonPressed hasnt this
        val ButtonMask.pressUp get() = _state.buttonPressed hasnt this && _prevState.buttonPressed has this

        val VRButtonId.press get() = _state.buttonPressed has this // if(buttonPressed && VRButtonId != 0)
        val VRButtonId.pressDown get() = _state.buttonPressed has this && _prevState.buttonPressed hasnt this
        val VRButtonId.pressUp get() = _state.buttonPressed hasnt this && _prevState.buttonPressed has this

        val ButtonMask.touch get() = _state.buttonTouched has this
        val ButtonMask.touchDown get() = _state.buttonTouched has this && _prevState.buttonTouched hasnt this
        val ButtonMask.touchUp get() = _state.buttonTouched hasnt this && _prevState.buttonTouched has this

        val VRButtonId.touch get() = _state.buttonTouched has this
        val VRButtonId.touchDown get() = _state.buttonTouched has this && _prevState.buttonTouched hasnt this
        val VRButtonId.touchUp get() = _state.buttonTouched hasnt this && _prevState.buttonTouched has this

        val _axis = Vec2()
        fun axis(buttonId: VRButtonId = VRButtonId.SteamVR_Touchpad): Vec2 {
            update()
            return _axis.apply {
                when (buttonId.i - VRButtonId.Axis0.i) {
                    0 -> put(_state.axis[0].x, _state.axis[0].y)
                    1 -> put(_state.axis[1].x, _state.axis[1].y)
                    2 -> put(_state.axis[2].x, _state.axis[2].y)
                    3 -> put(_state.axis[3].x, _state.axis[3].y)
                    4 -> put(_state.axis[4].x, _state.axis[4].y)
                    else -> put(0)
                }
            }
        }

        fun triggerHapticPulse(durationMicroSec: Int = 500, buttonId: VRButtonId = VRButtonId.SteamVR_Touchpad) =
                hmd.triggerHapticPulse(index, buttonId.i - VRButtonId.Axis0.i, durationMicroSec)


        val triggerMode get() = if (left) leftTriggerMode else rightTriggerMode
        /** amount trigger must be pulled or released to change state   */
        val triggerDelta get() = if (left) leftTriggerDelta else rightTriggerDelta
        private var triggerLimit = 0f
        private var triggerPrevLimit = triggerLimit
        private var triggerState = false
        private var triggerPrevState = triggerState

        fun updateTrigger() {
            if (ButtonMask.SteamVR_Trigger.touch) when (triggerMode) {
                TriggerMode.Off -> Unit
                else -> {
                    triggerPrevState = triggerState
                    val value = _state.axis[1].x  // trigger
                    if (triggerState) {
                        if (value < triggerLimit - triggerDelta || value <= 0f)
                            triggerState = false
                    } else if (value > triggerLimit + triggerDelta || value >= 1f)
                        triggerState = true
                    triggerLimit = if (triggerState) glm.max(triggerLimit, value) else glm.min(triggerLimit, value)
                    if (when (triggerMode) {
                                TriggerMode.OnState -> triggerPrevState != triggerState
                                TriggerMode.OnLimit ->
                                    if (triggerPrevLimit != triggerLimit) {
                                        triggerPrevLimit = triggerLimit
                                        true
                                    } else false
                                else -> true    // remains only Always at this point, Off has been excluded by the previous `when`
                            })
                        triggerMove(left, triggerState, triggerLimit, value)
                }
            }
        }

        val trigger get() = update().let { triggerState }
        val triggerDown get() = update().let { triggerState && !triggerPrevState }
        val triggerUp get() = update().let { !triggerState && triggerPrevState }

        private infix fun Long.has(buttonMask: ButtonMask) = and(buttonMask.i) != 0L
        private infix fun Long.hasnt(buttonMask: ButtonMask) = and(buttonMask.i) == 0L
        private infix fun Long.has(buttonId: VRButtonId) = and(buttonId.mask) != 0L
        private infix fun Long.hasnt(buttonId: VRButtonId) = and(buttonId.mask) == 0L

        fun dispose() {
            _state.free()
            _prevState.free()
            _pose.free()
        }
    }
}