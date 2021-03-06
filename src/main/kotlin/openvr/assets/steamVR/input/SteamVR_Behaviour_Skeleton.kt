package openvr.assets.steamVR.input

import glm_.func.rad
import glm_.glm
import glm_.quat.Quat
import glm_.vec3.Vec3
import kotlinx.coroutines.Job
import openvr.lib.TrackingResult
import openvr.lib.VRSkeletalMotionRange
import openvr.lib.vrInput
import openvr.plugin2.SteamVR_Input_Sources
import openvr.plugin2.Transform

typealias SteamVR_Behaviour_SkeletonEvent = (SteamVR_Behaviour_Skeleton, SteamVR_Input_Sources) -> Unit
typealias SteamVR_Behaviour_Skeleton_ConnectedChangedEvent = (SteamVR_Behaviour_Skeleton, SteamVR_Input_Sources, Boolean) -> Unit
typealias SteamVR_Behaviour_Skeleton_TrackingChangedEvent = (SteamVR_Behaviour_Skeleton, SteamVR_Input_Sources, TrackingResult) -> Unit

typealias SteamVR_Behaviour_Skeleton_ActiveChangeHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources, active: Boolean) -> Unit
typealias SteamVR_Behaviour_Skeleton_ChangeHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources) -> Unit
typealias SteamVR_Behaviour_Skeleton_UpdateHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources) -> Unit
typealias SteamVR_Behaviour_Skeleton_TrackingChangeHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources, trackingState: TrackingResult) -> Unit
typealias SteamVR_Behaviour_Skeleton_ValidPoseChangeHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources, validPose: Boolean) -> Unit
typealias SteamVR_Behaviour_Skeleton_DeviceConnectedChangeHandler = (fromAction: SteamVR_Behaviour_Skeleton, inputSource: SteamVR_Input_Sources, deviceConnected: Boolean) -> Unit

class SteamVR_Behaviour_Skeleton {

    /** If not set, will try to auto assign this based on 'Skeleton' + inputSource
     *  The action this component will use to update the model. Must be a Skeleton type action. */
    var skeletonAction: SteamVR_Action_Skeleton? = null

    /** The device this action should apply to. Any if the action is not device specific. */
    var inputSource: SteamVR_Input_Sources? = null

    /** The range of motion you'd like the hand to move in. With controller is the best estimate of the fingers wrapped around a controller. Without is from a flat hand to a fist. */
    var rangeOfMotion = VRSkeletalMotionRange.WithoutController

    /** The root Transform of the skeleton. Needs to have a child (wrist) then wrist should have children in the order thumb, index, middle, ring, pinky */
    var skeletonRoot: Transform? = null

    /** The transform this transform should be relative to
     *  If not set, relative to parent */
    var origin: Transform? = null

    /** Whether or not to update this transform's position and rotation inline with the skeleton transforms or if this is handled in another script */
    var updatePose = true

    /** Check this to not set the positions of the bones. This is helpful for differently scaled skeletons. */
    var onlySetRotations = false

    /** How much of a blend to apply to the transform positions and rotations.
     *  Set to 0 for the transform orientation to be set by an animation.
     *  Set to 1 for the transform orientation to be set by the skeleton action. */
    var skeletonBlend = 1f

    /** This Unity event will fire whenever the position or rotation of the bones are updated. */
    var onBoneTransformsUpdated: SteamVR_Behaviour_SkeletonEvent? = null

    /** This Unity event will fire whenever the position or rotation of this transform is updated. */
    var onTransformUpdated: SteamVR_Behaviour_SkeletonEvent? = null

    /** This Unity event will fire whenever the position or rotation of this transform is changed. */
    var onTransformChanged: SteamVR_Behaviour_SkeletonEvent? = null

    /** This Unity event will fire whenever the device is connected or disconnected */
    var onConnectedChanged: SteamVR_Behaviour_Skeleton_ConnectedChangedEvent? = null

    /** This Unity event will fire whenever the device's tracking state changes */
    var onTrackingChanged: SteamVR_Behaviour_Skeleton_TrackingChangedEvent? = null


    /** This C# event will fire whenever the position or rotation of this transform is updated. */
    var onBoneTransformsUpdatedEvent: SteamVR_Behaviour_Skeleton_UpdateHandler? = null

    /** This C# event will fire whenever the position or rotation of this transform is updated. */
    var onTransformUpdatedEvent: SteamVR_Behaviour_Skeleton_UpdateHandler? = null

    /** This C# event will fire whenever the position or rotation of this transform is changed. */
    var onTransformChangedEvent: SteamVR_Behaviour_Skeleton_ChangeHandler? = null

    /** This C# event will fire whenever the device is connected or disconnected */
    var onConnectedChangedEvent: SteamVR_Behaviour_Skeleton_DeviceConnectedChangeHandler? = null

    /** This C# event will fire whenever the device's tracking state changes */
    var onTrackingChangedEvent: SteamVR_Behaviour_Skeleton_TrackingChangeHandler? = null


    /** Can be set to mirror the bone data across the x axis
     *  "Is this rendermodel a mirror of another one?" */
    lateinit var mirroring: MirrorType

//    [Header("No Skeleton - Fallback")]
//
//
    /** [Tooltip("The fallback SkeletonPoser to drive hand animation when no skeleton data is available")]
     *  The fallback SkeletonPoser to drive hand animation when no skeleton data is available */
    var fallbackPoser: SteamVR_Skeleton_Poser? = null

    /** [Tooltip("The fallback action to drive finger curl values when no skeleton data is available")]
     *  The fallback SkeletonPoser to drive hand animation when no skeleton data is available */
    var fallbackCurlAction: SteamVR_Action_Single? = null

    /** Is the skeleton action bound? */
    val skeletonAvailable: Boolean
        get() = skeletonAction!!.activeBinding


    /** The current skeletonPoser we're getting pose data from */
    protected var blendPoser: SteamVR_Skeleton_Poser? = null
    /** The current pose snapshot */
    protected var blendSnapshot: SteamVR_Skeleton_PoseSnapshot? = null


    /** @Returns whether this action is bound and the action set is active */
    val isActive: Boolean
        get() = skeletonAction!!.GetActive()


    /** An array of five 0-1 values representing how curled a finger is. 0 being straight, 1 being fully curled. 0 being thumb, 4 being pinky */
    val fingerCurls: FloatArray
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurls()
            //fallback, return array where each finger curl is just the fallback curl action value
            else -> FloatArray(5) { fallbackCurlAction!!.getAxis(inputSource!!) }
        }

    /** An 0-1 value representing how curled a finger is. 0 being straight, 1 being fully curled. */
    val thumbCurl: Float
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurl(FingerIndex.thumb)
            else -> fallbackCurlAction!!.getAxis(inputSource!!)
        }

    /** An 0-1 value representing how curled a finger is. 0 being straight, 1 being fully curled. */
    val indexCurl: Float
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurl(FingerIndex.index)
            else -> fallbackCurlAction!!.getAxis(inputSource!!)
        }

    /** An 0-1 value representing how curled a finger is. 0 being straight, 1 being fully curled. */
    val middleCurl: Float
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurl(FingerIndex.middle)
            else -> fallbackCurlAction!!.getAxis(inputSource!!)
        }

    /** An 0-1 value representing how curled a finger is. 0 being straight, 1 being fully curled. */
    val ringCurl: Float
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurl(FingerIndex.ring)
            else -> fallbackCurlAction!!.getAxis(inputSource!!)
        }

    /** An 0-1 value representing how curled a finger is. 0 being straight, 1 being fully curled. */
    val pinkyCurl: Float
        get() = when {
            skeletonAvailable -> skeletonAction!!.getFingerCurl(FingerIndex.pinky)
            else -> fallbackCurlAction!!.getAxis(inputSource!!)
        }


    val root: Transform
        get() = bones[JointIndex.root.i]!!
    val wrist: Transform
        get() = bones[JointIndex.wrist.i]!!
    val indexMetacarpal: Transform
        get() = bones[JointIndex.indexMetacarpal.i]!!
    val indexProximal: Transform
        get() = bones[JointIndex.indexProximal.i]!!
    val indexMiddle: Transform
        get() = bones[JointIndex.indexMiddle.i]!!
    val indexDistal: Transform
        get() = bones[JointIndex.indexDistal.i]!!
    val indexTip: Transform
        get() = bones[JointIndex.indexTip.i]!!
    val middleMetacarpal: Transform
        get() = bones[JointIndex.middleMetacarpal.i]!!
    val middleProximal: Transform
        get() = bones[JointIndex.middleProximal.i]!!
    val middleMiddle: Transform
        get() = bones[JointIndex.middleMiddle.i]!!
    val middleDistal: Transform
        get() = bones[JointIndex.middleDistal.i]!!
    val middleTip: Transform
        get() = bones[JointIndex.middleTip.i]!!
    val pinkyMetacarpal: Transform
        get() = bones[JointIndex.pinkyMetacarpal.i]!!
    val pinkyProximal: Transform
        get() = bones[JointIndex.pinkyProximal.i]!!
    val pinkyMiddle: Transform
        get() = bones[JointIndex.pinkyMiddle.i]!!
    val pinkyDistal: Transform
        get() = bones[JointIndex.pinkyDistal.i]!!
    val pinkyTip: Transform
        get() = bones[JointIndex.pinkyTip.i]!!
    val ringMetacarpal: Transform
        get() = bones[JointIndex.ringMetacarpal.i]!!
    val ringProximal: Transform
        get() = bones[JointIndex.ringProximal.i]!!
    val ringMiddle: Transform
        get() = bones[JointIndex.ringMiddle.i]!!
    val ringDistal: Transform
        get() = bones[JointIndex.ringDistal.i]!!
    val ringTip: Transform
        get() = bones[JointIndex.ringTip.i]!!
    val thumbMetacarpal: Transform
        get() = bones[JointIndex.thumbMetacarpal.i]!! //doesn't exist - mapped to proximal
    val thumbProximal: Transform
        get() = bones[JointIndex.thumbProximal.i]!!
    val thumbMiddle: Transform
        get() = bones[JointIndex.thumbMiddle.i]!!
    val thumbDistal: Transform
        get() = bones[JointIndex.thumbDistal.i]!!
    val thumbTip: Transform
        get() = bones[JointIndex.thumbTip.i]!!
    val thumbAux: Transform
        get() = bones[JointIndex.thumbAux.i]!!
    val indexAux: Transform
        get() = bones[JointIndex.indexAux.i]!!
    val middleAux: Transform
        get() = bones[JointIndex.middleAux.i]!!
    val ringAux: Transform
        get() = bones[JointIndex.ringAux.i]!!
    val pinkyAux: Transform
        get() = bones[JointIndex.pinkyAux.i]!!

    /** An array of all the finger proximal joint transforms */
    lateinit var proximals: Array<Transform>
        protected set

    /** An array of all the finger middle joint transforms */
    lateinit var middles: Array<Transform>
        protected set

    /** An array of all the finger distal joint transforms */
    lateinit var distals: Array<Transform>
        protected set

    /** An array of all the finger tip transforms */
    lateinit var tips: Array<Transform>
        protected set

    /** An array of all the finger aux transforms */
    lateinit var auxs: Array<Transform>
        protected set

    protected var blendRoutine: Job? = null
    protected var rangeOfMotionBlendRoutine: Job? = null
    protected var attachRoutine: Job? = null

    protected lateinit var bones: Array<Transform?>

    /** The range of motion that is set temporarily (call ResetTemporaryRangeOfMotion to reset to rangeOfMotion) */
    protected var temporaryRangeOfMotion: VRSkeletalMotionRange? = null

    /** Get the accuracy level of the skeletal tracking data:
     *      Estimated: Body part location can’t be directly determined by the device. Any skeletal pose provided by the
     *          device is estimated based on the active buttons, triggers, joysticks, or other input sensors.
     *          Examples include the Vive Controller and gamepads.
     *      Partial: Body part location can be measured directly but with fewer degrees of freedom than the actual body
     *          part.Certain body part positions may be unmeasured by the device and estimated from other input data.
     *          Examples include Knuckles or gloves that only measure finger curl
     *      Full: Body part location can be measured directly throughout the entire range of motion of the body part.
     *          Examples include hi-end mocap systems, or gloves that measure the rotation of each finger segment. */
    val skeletalTrackingLevel: vrInput.VRSkeletalTrackingLevel
        get() = when {
            skeletonAvailable -> skeletonAction!!.skeletalTrackingLevel
            else -> vrInput.VRSkeletalTrackingLevel.Estimated
        }

    /** @Returns true if we are in the process of blending the skeletonBlend field (between animation and bone data) */
//    val isBlending: Boolean
//        get() = blendRoutine != null

    /*
        public float predictedSecondsFromNow
        {
            get
            {
                return skeletonAction.predictedSecondsFromNow;
            }
            set
            {
                skeletonAction.predictedSecondsFromNow = value;
            }
        }
        */

    val actionSet: SteamVR_ActionSet?
        get() = skeletonAction!!.actionSet

    val direction: SteamVR_ActionDirections
        get() = skeletonAction!!.direction

    protected fun awake() {

        assignBonesArray()

        proximals = arrayOf(thumbProximal, indexProximal, middleProximal, ringProximal, pinkyProximal)
        middles = arrayOf(thumbMiddle, indexMiddle, middleMiddle, ringMiddle, pinkyMiddle)
        distals = arrayOf(thumbDistal, indexDistal, middleDistal, ringDistal, pinkyDistal)
        tips = arrayOf(thumbTip, indexTip, middleTip, ringTip, pinkyTip)
        auxs = arrayOf(thumbAux, indexAux, middleAux, ringAux, pinkyAux)

        checkSkeletonAction()
    }

    protected fun checkSkeletonAction() {
        if (skeletonAction == null)
            skeletonAction = SteamVR_Input.getAction<SteamVR_Action_Skeleton>(ActionType.Skeleton, "Skeleton ${inputSource!!}")
    }

    protected fun assignBonesArray() {
        TODO()
//        bones = skeletonRoot.getComponentsInChildren<Transform>()
    }

    protected fun onEnable() {
        checkSkeletonAction()
        SteamVR_Input.onSkeletonsUpdated += ::onSkeletonsUpdated

        skeletonAction?.let {
            it.onDeviceConnectedChanged += ::onDeviceConnectedChanged
            it.onTrackingChanged += ::onTrackingChanged
        }
    }

    protected fun onDisable() {
        SteamVR_Input.onSkeletonsUpdated -= ::onSkeletonsUpdated

        skeletonAction?.let {
            it.onDeviceConnectedChanged -= ::onDeviceConnectedChanged
            it.onTrackingChanged -= ::onTrackingChanged
        }
    }

    private fun onDeviceConnectedChanged(fromAction: SteamVR_Action_Skeleton, deviceConnected: Boolean) {
        onConnectedChanged?.invoke(this, inputSource!!, deviceConnected)
        onConnectedChangedEvent?.invoke(this, inputSource!!, deviceConnected)
    }

    private fun onTrackingChanged(fromAction: SteamVR_Action_Skeleton, trackingState: TrackingResult) {
        onTrackingChanged?.invoke(this, inputSource!!, trackingState)
        onTrackingChangedEvent?.invoke(this, inputSource!!, trackingState)
    }

    protected fun onSkeletonsUpdated(skipSendingEvents: Boolean) = updateSkeleton()

    protected fun updateSkeleton() {
        if (skeletonAction == null)
            return

        if (updatePose)
            updatePose()

        blendPoser?.let {
            if (skeletonBlend < 1) {
                if (blendSnapshot == null)
                    blendSnapshot = it.getBlendedPose(this)
                blendSnapshot = it.getBlendedPose(this)
            }
        }

        if (rangeOfMotionBlendRoutine == null) {
            skeletonAction!!.rangeOfMotion = temporaryRangeOfMotion ?: rangeOfMotion //this may be a frame behind
            updateSkeletonTransforms()
        }
    }

    /** Sets a temporary range of motion for this action that can easily be reset (using ResetTemporaryRangeOfMotion).
     *  This is useful for short range of motion changes, for example picking up a controller shaped object
     *  @param newRangeOfMotion: The new range of motion you want to apply (temporarily)
     *  @param blendOverSeconds: How long you want the blend to the new range of motion to take (in seconds) */
    fun setTemporaryRangeOfMotion(newRangeOfMotion: VRSkeletalMotionRange, blendOverSeconds: Float = 0.1f) {
        if (rangeOfMotion != newRangeOfMotion || temporaryRangeOfMotion != newRangeOfMotion)
            temporaryRangeOfMotionBlend(newRangeOfMotion, blendOverSeconds)
    }

    /** Resets the previously set temporary range of motion.
     *  Will return to the range of motion defined by the rangeOfMotion field.
     *  @param blendOverSeconds: How long you want the blend to the standard range of motion to take (in seconds) */
    fun resetTemporaryRangeOfMotion(blendOverSeconds: Float = 0.1f) = resetTemporaryRangeOfMotionBlend(blendOverSeconds)

    /** Permanently sets the range of motion for this component.
     *
     *  @param newRangeOfMotion: The new range of motion to be set.
     *      WithController being the best estimation of where fingers are wrapped around the controller (pressing buttons, etc).
     *      WithoutController being a range between a flat hand and a fist.
     *  @param blendOverSeconds: How long you want the blend to the new range of motion to take (in seconds) */
    fun setRangeOfMotion(newRangeOfMotion: VRSkeletalMotionRange, blendOverSeconds: Float = 0.1f) {
        if (rangeOfMotion != newRangeOfMotion)
            rangeOfMotionBlend(newRangeOfMotion, blendOverSeconds)
    }

    /** Blend from the current skeletonBlend amount to full bone data. (skeletonBlend = 1)
     *  @param overTime: How long you want the blend to take (in seconds) */
    fun blendToSkeleton(overTime: Float = 0.1f) {
        blendPoser?.let { blendSnapshot = it.getBlendedPose(this) }
        blendPoser = null
        blendTo(1f, overTime)
    }

    /** Blend from the current skeletonBlend amount to pose animation. (skeletonBlend = 0)
     *  Note: This will ignore the root position and rotation of the pose.
     *  @param overTime: How long you want the blend to take (in seconds) */
    fun blendToPoser(poser: SteamVR_Skeleton_Poser?, overTime: Float = 0.1f) {
        if (poser == null)
            return

        blendPoser = poser
        blendTo(0f, overTime)
    }

    /** Blend from the current skeletonBlend amount to full animation data (no bone data. skeletonBlend = 0)
     *  @param overTime: How long you want the blend to take (in seconds) */
    fun blendToAnimation(overTime: Float = 0.1f) = blendTo(0f, overTime)

    /** Blend from the current skeletonBlend amount to a specified new amount.
     *  @param blendToAmount: The amount of blend you want to apply.
     *      0 being fully set by animations, 1 being fully set by bone data from the action.
     *  @param overTime: How long you want the blend to take (in seconds) */
    fun blendTo(blendToAmount: Float, overTime: Float) {
        TODO()
//        if (blendRoutine != null)
//            StopCoroutine(blendRoutine)
//
//        if (this.gameObject.activeInHierarchy)
//            blendRoutine = StartCoroutine(DoBlendRoutine(blendToAmount, overTime))
    }


    protected suspend fun doBlendRoutine(blendToAmount: Float, overTime: Float) {
        TODO()
//        val startTime = Time.time
//        val endTime = startTime + overTime
//
//        val startAmount = skeletonBlend
//
//        while (Time.time < endTime) {
//            yield()
//            skeletonBlend = lerp(startAmount, blendToAmount, (Time.time - startTime) / overTime)
//        }
//
//        skeletonBlend = blendToAmount
//        blendRoutine = null
    }

    protected fun rangeOfMotionBlend(newRangeOfMotion: VRSkeletalMotionRange, blendOverSeconds: Float) {
        TODO()
//        if (rangeOfMotionBlendRoutine != null)
//            StopCoroutine(rangeOfMotionBlendRoutine)
//
//        EVRSkeletalMotionRange oldRangeOfMotion = rangeOfMotion
//                rangeOfMotion = newRangeOfMotion
//
//        if (this.gameObject.activeInHierarchy) {
//            rangeOfMotionBlendRoutine = StartCoroutine(DoRangeOfMotionBlend(oldRangeOfMotion, newRangeOfMotion, blendOverSeconds))
//        }
    }

    protected fun temporaryRangeOfMotionBlend(newRangeOfMotion: VRSkeletalMotionRange, blendOverSeconds: Float) {
        TODO()
//        if (rangeOfMotionBlendRoutine != null)
//            StopCoroutine(rangeOfMotionBlendRoutine)
//
//        EVRSkeletalMotionRange oldRangeOfMotion = rangeOfMotion
//                if (temporaryRangeOfMotion != null)
//                    oldRangeOfMotion = temporaryRangeOfMotion.Value
//
//        temporaryRangeOfMotion = newRangeOfMotion
//
//        if (this.gameObject.activeInHierarchy) {
//            rangeOfMotionBlendRoutine = StartCoroutine(DoRangeOfMotionBlend(oldRangeOfMotion, newRangeOfMotion, blendOverSeconds))
//        }
    }

    protected fun resetTemporaryRangeOfMotionBlend(blendOverSeconds: Float) {
        TODO()
//        if (temporaryRangeOfMotion != null) {
//            if (rangeOfMotionBlendRoutine != null)
//                StopCoroutine(rangeOfMotionBlendRoutine)
//
//            EVRSkeletalMotionRange oldRangeOfMotion = temporaryRangeOfMotion . Value
//
//                    EVRSkeletalMotionRange newRangeOfMotion = rangeOfMotion
//
//                    temporaryRangeOfMotion = null
//
//            if (this.gameObject.activeInHierarchy) {
//                rangeOfMotionBlendRoutine = StartCoroutine(DoRangeOfMotionBlend(oldRangeOfMotion, newRangeOfMotion, blendOverSeconds))
//            }
//        }
    }

    protected suspend fun doRangeOfMotionBlend(oldRangeOfMotion: VRSkeletalMotionRange,
                                               newRangeOfMotion: VRSkeletalMotionRange, overTime: Float) {
        TODO()
//        float startTime = Time . time
//                float endTime = startTime +overTime
//
//        Vector3[] oldBonePositions;
//        Quaternion[] oldBoneRotations;
//
//        Vector3[] newBonePositions;
//        Quaternion[] newBoneRotations;
//
//                while (Time.time < endTime) {
//                    `yield` return null
//                    float lerp =(Time.time - startTime) / overTime
//
//                    if (skeletonBlend > 0) {
//                        skeletonAction.SetRangeOfMotion(oldRangeOfMotion)
//                        skeletonAction.UpdateValueWithoutEvents()
//                        oldBonePositions = (Vector3[])GetBonePositions().Clone();
//                        oldBoneRotations = (Quaternion[])GetBoneRotations().Clone();
//
//                        skeletonAction.SetRangeOfMotion(newRangeOfMotion)
//                        skeletonAction.UpdateValueWithoutEvents()
//                        newBonePositions = GetBonePositions();
//                        newBoneRotations = GetBoneRotations()
//
//                        for (int boneIndex = 0; boneIndex < bones.Length; boneIndex++)
//                        {
//                            if (bones[boneIndex] == null)
//                                continue
//
//                            if (SteamVR_Utils.IsValid(newBoneRotations[boneIndex]) == false || SteamVR_Utils.IsValid(oldBoneRotations[boneIndex]) == false) {
//                                continue
//                            }
//
//                            Vector3 blendedRangeOfMotionPosition = Vector3 . Lerp (oldBonePositions[boneIndex], newBonePositions[boneIndex], lerp)
//                            Quaternion blendedRangeOfMotionRotation = Quaternion . Lerp (oldBoneRotations[boneIndex], newBoneRotations[boneIndex], lerp)
//
//                            if (skeletonBlend < 1) {
//                                if (blendPoser != null) {
//                                    SetBonePosition(boneIndex, Vector3.Lerp(blendSnapshot.bonePositions[boneIndex], blendedRangeOfMotionPosition, skeletonBlend))
//                                    SetBoneRotation(boneIndex, Quaternion.Lerp(GetBlendPoseForBone(boneIndex, blendedRangeOfMotionRotation), blendedRangeOfMotionRotation, skeletonBlend))
//                                } else {
//                                    SetBonePosition(boneIndex, Vector3.Lerp(bones[boneIndex].localPosition, blendedRangeOfMotionPosition, skeletonBlend))
//                                    SetBoneRotation(boneIndex, Quaternion.Lerp(bones[boneIndex].localRotation, blendedRangeOfMotionRotation, skeletonBlend))
//                                }
//                            } else {
//                                SetBonePosition(boneIndex, blendedRangeOfMotionPosition)
//                                SetBoneRotation(boneIndex, blendedRangeOfMotionRotation)
//                            }
//                        }
//                    }
//
//                    if (onBoneTransformsUpdated != null)
//                        onBoneTransformsUpdated.Invoke(this, inputSource)
//                    if (onBoneTransformsUpdatedEvent != null)
//                        onBoneTransformsUpdatedEvent.Invoke(this, inputSource)
//                }
//
//        rangeOfMotionBlendRoutine = null
    }

    //why does this exist
    protected fun getBlendPoseForBone(boneIndex: Int, skeletonRotation: Quat): Quat =
            blendSnapshot!!.boneRotations[boneIndex]

    fun updateSkeletonTransforms() {

        val bonePositions = getBonePositions()
        val boneRotations = getBoneRotations()

        if (skeletonBlend <= 0)
            blendPoser?.let {
                val mainPose = it.skeletonMainPose.getHand(inputSource!!)
                for (boneIndex in bones.indices) {
                    if (bones[boneIndex] == null)
                        continue

                    if ((boneIndex == JointIndex.wrist.i && mainPose!!.ignoreWristPoseData) ||
                            (boneIndex == JointIndex.root.i && mainPose!!.ignoreRootPoseData)) {
                        setBonePosition(boneIndex, bonePositions[boneIndex])
                        setBoneRotation(boneIndex, boneRotations[boneIndex])
                    } else {
                        val poseRotation = getBlendPoseForBone(boneIndex, boneRotations[boneIndex])
                        setBonePosition(boneIndex, blendSnapshot!!.bonePositions[boneIndex])
                        setBoneRotation(boneIndex, poseRotation)
                    }
                }
            } ?: run {
                for (boneIndex in bones.indices) {
                    val poseRotation = getBlendPoseForBone(boneIndex, boneRotations[boneIndex])
                    setBonePosition(boneIndex, blendSnapshot!!.bonePositions[boneIndex])
                    setBoneRotation(boneIndex, poseRotation)
                }
            }
        else if (skeletonBlend >= 1) {
            for (boneIndex in bones.indices) {

                if (bones[boneIndex] == null)
                    continue

                setBonePosition(boneIndex, bonePositions[boneIndex])
                setBoneRotation(boneIndex, boneRotations[boneIndex])
            }
        } else {
            for (boneIndex in bones.indices) {

                if (bones[boneIndex] == null)
                    continue

                when {
                    blendPoser != null -> {
                        val mainPose = blendPoser!!.skeletonMainPose.getHand(inputSource!!)

                        if ((boneIndex == JointIndex.wrist.i && mainPose!!.ignoreWristPoseData) ||
                                (boneIndex == JointIndex.root.i && mainPose!!.ignoreRootPoseData)) {
                            setBonePosition(boneIndex, bonePositions[boneIndex])
                            setBoneRotation(boneIndex, boneRotations[boneIndex])
                        } else {
                            //Quaternion poseRotation = GetBlendPoseForBone(boneIndex, boneRotations[boneIndex]);
                            setBonePosition(boneIndex, glm.mix(blendSnapshot!!.bonePositions[boneIndex], bonePositions[boneIndex], skeletonBlend))
                            setBoneRotation(boneIndex, glm.lerp(blendSnapshot!!.boneRotations[boneIndex], boneRotations[boneIndex], skeletonBlend))
                            //SetBoneRotation(boneIndex, GetBlendPoseForBone(boneIndex, boneRotations[boneIndex]));
                        }
                    }
                    blendSnapshot == null -> {
                        setBonePosition(boneIndex, glm.mix(bones[boneIndex]!!.localPosition, bonePositions[boneIndex], skeletonBlend))
                        setBoneRotation(boneIndex, glm.lerp(bones[boneIndex]!!.localRotation, boneRotations[boneIndex], skeletonBlend))
                    }
                    else -> {
                        setBonePosition(boneIndex, glm.mix(blendSnapshot!!.bonePositions[boneIndex], bonePositions[boneIndex], skeletonBlend))
                        setBoneRotation(boneIndex, glm.lerp(blendSnapshot!!.boneRotations[boneIndex], boneRotations[boneIndex], skeletonBlend))
                    }
                }
            }
        }


        onBoneTransformsUpdated?.invoke(this, inputSource!!)
        onBoneTransformsUpdatedEvent?.invoke(this, inputSource!!)
    }

    fun setBonePosition(boneIndex: Int, localPosition: Vec3) {
        if (!onlySetRotations) //ignore position sets if we're only setting rotations
            bones[boneIndex]!!.localPosition put localPosition
    }

    fun setBoneRotation(boneIndex: Int, localRotation: Quat) =
            bones[boneIndex]!!.localRotation put localRotation

    /** Gets the transform for a bone by the joint index. Joint indexes specified in: SteamVR_Skeleton_JointIndexes
     *  @param joint: The joint index of the bone. Specified in SteamVR_Skeleton_JointIndexes */
    fun getBone(joint: Int): Transform {
        if (!::bones.isInitialized || bones.isEmpty())
            awake()

        return bones[joint]!!
    }


    /** Gets the position of the transform for a bone by the joint index. Joint indexes specified in: SteamVR_Skeleton_JointIndexes
     *  @param joint: The joint index of the bone. Specified in SteamVR_Skeleton_JointIndexes
     *  @param local: true to get the localspace position for the joint (position relative to this joint's parent) */
    fun getBonePosition(joint: Int, local: Boolean = false): Vec3 = when {
        local -> bones[joint]!!.localPosition
        else -> bones[joint]!!.position
    }

    /** Gets the rotation of the transform for a bone by the joint index. Joint indexes specified in: SteamVR_Skeleton_JointIndexes
     *  @param joint: The joint index of the bone. Specified in SteamVR_Skeleton_JointIndexes
     *  @param local: true to get the localspace rotation for the joint (rotation relative to this joint's parent) */
    fun getBoneRotation(joint: Int, local: Boolean = false): Quat = when {
        local -> bones[joint]!!.localRotation
        else -> bones[joint]!!.rotation
    }

    protected fun getBonePositions(): Array<Vec3> = when {
        skeletonAvailable -> {
            val rawSkeleton = skeletonAction!!.getBonePositions()
            if (mirroring == MirrorType.LeftToRight || mirroring == MirrorType.RightToLeft)
                for (boneIndex in rawSkeleton.indices)
                    rawSkeleton[boneIndex] = mirrorPosition(boneIndex, rawSkeleton[boneIndex])
            rawSkeleton
        }
        //fallback to getting skeleton pose from skeletonPoser
        else -> fallbackPoser?.getBlendedPose(skeletonAction!!, inputSource!!)?.bonePositions
                ?: throw Error("Skeleton Action is not bound, and you have not provided a fallback SkeletonPoser. Please create one to drive hand animation when no skeleton data is available.")
    }

    protected fun getBoneRotations(): Array<Quat> = when {
        skeletonAvailable -> {
            val rawSkeleton = skeletonAction!!.getBoneRotations()

            if (mirroring == MirrorType.LeftToRight || mirroring == MirrorType.RightToLeft)
                for (boneIndex in rawSkeleton.indices)
                    rawSkeleton[boneIndex] = mirrorRotation(boneIndex, rawSkeleton[boneIndex])
            rawSkeleton
        }
        //fallback to getting skeleton pose from skeletonPoser
        else -> fallbackPoser?.getBlendedPose(skeletonAction!!, inputSource!!)?.boneRotations
                ?: throw Error("Skeleton Action is not bound, and you have not provided a fallback SkeletonPoser. Please create one to drive hand animation when no skeleton data is available.")
    }

    protected fun updatePose() {

        if (skeletonAction == null) return

        val skeletonPosition = skeletonAction!!.localPosition
        val skeletonRotation = skeletonAction!!.localRotation
        TODO()
//        if (origin == null) {
//            if (this.transform.parent != null) {
//                skeletonPosition = this.transform.parent.TransformPoint(skeletonPosition)
//                skeletonRotation = this.transform.parent.rotation * skeletonRotation
//            }
//        } else {
//            skeletonPosition = origin.TransformPoint(skeletonPosition)
//            skeletonRotation = origin.rotation * skeletonRotation
//        }
//
//        if (skeletonAction.poseChanged) {
//            if (onTransformChanged != null)
//                onTransformChanged.Invoke(this, inputSource)
//            if (onTransformChangedEvent != null)
//                onTransformChangedEvent.Invoke(this, inputSource)
//        }
//
//        this.transform.position = skeletonPosition
//        this.transform.rotation = skeletonRotation
//
//        if (onTransformUpdated != null)
//            onTransformUpdated.Invoke(this, inputSource)
    }

    /** @Returns an array of positions/rotations that represent the state of each bone in a reference pose.
     *  @param referencePose: Which reference pose to return */
    fun forceToReferencePose(referencePose: vrInput.VRSkeletalReferencePose) {
        TODO()
//        var temporarySession = false
//        if (Application.isEditor && Application.isPlaying == false) {
//            temporarySession = SteamVR.InitializeTemporarySession(true)
//            Awake()
//
//            #if UNITY_EDITOR
//            //gotta wait a bit for steamvr input to startup //todo: implement steamvr_input.isready
//            string title = "SteamVR"
//            string text = "Getting reference pose..."
//            float msToWait = 3000
//            float increment = 100
//            for (float timer = 0; timer < msToWait; timer += increment)
//            {
//                bool cancel = UnityEditor . EditorUtility . DisplayCancelableProgressBar (title, text, timer / msToWait)
//                if (cancel) {
//                    UnityEditor.EditorUtility.ClearProgressBar()
//
//                    if (temporarySession)
//                        SteamVR.ExitTemporarySession()
//                    return
//                }
//                System.Threading.Thread.Sleep((int) increment)
//            }
//            UnityEditor.EditorUtility.ClearProgressBar()
//            #endif
//
//            skeletonAction.actionSet.Activate()
//
//            SteamVR_ActionSet_Manager.UpdateActionStates(true)
//
//            skeletonAction.UpdateValueWithoutEvents()
//        }
//
//        if (skeletonAction.active == false) {
//            Debug.LogError("<b>[SteamVR Input]</b> Please turn on your " + inputSource.ToString() + " controller and ensure SteamVR is open.")
//            return
//        }
//
//        SteamVR_Utils.RigidTransform[] transforms = skeletonAction . GetReferenceTransforms (EVRSkeletalTransformSpace.Parent, referencePose)
//
//        if (transforms == null || transforms.Length == 0) {
//            Debug.LogError("<b>[SteamVR Input]</b> Unable to get the reference transform for " + inputSource.ToString() + ". Please make sure SteamVR is open and both controllers are connected.")
//        }
//
//        if (mirroring == MirrorType.LeftToRight || mirroring == MirrorType.RightToLeft)
//        {
//        for (int boneIndex = 0; boneIndex < transforms.Length; boneIndex++)
//        {
//            bones[boneIndex].localPosition = MirrorPosition(boneIndex, transforms[boneIndex].pos);
//            bones[boneIndex].localRotation = MirrorRotation(boneIndex, transforms[boneIndex].rot);
//        }
//        }
//        else
//        {
//            for (int boneIndex = 0; boneIndex < transforms.Length; boneIndex++)
//            {
//                bones[boneIndex].localPosition = transforms[boneIndex].pos;
//                bones[boneIndex].localRotation = transforms[boneIndex].rot;
//            }
//        }
//
//        if (temporarySession)
//            SteamVR.ExitTemporarySession()
    }

    companion object {

        fun mirrorPosition(boneIndex: Int, rawPosition: Vec3): Vec3 = when {
            boneIndex == JointIndex.wrist.i || isMetacarpal(boneIndex) -> rawPosition.times(-1, 1, 1)
            boneIndex != JointIndex.root.i -> rawPosition.negate()
            else -> Vec3(rawPosition)
        }

        private val rightFlipAngle = glm.angleAxis(180.rad, Vec3(1, 0, 0))

        fun mirrorRotation(boneIndex: Int, rawRotation: Quat): Quat = when {
            boneIndex == JointIndex.wrist.i -> Quat(rawRotation).apply {
                y *= -1
                z *= -1
            }
            isMetacarpal(boneIndex) -> rightFlipAngle * rawRotation
            else -> Quat(rawRotation)
        }

        protected fun isMetacarpal(boneIndex: Int): Boolean =
                boneIndex == JointIndex.indexMetacarpal.i || boneIndex == JointIndex.middleMetacarpal.i ||
                        boneIndex == JointIndex.ringMetacarpal.i || boneIndex == JointIndex.pinkyMetacarpal.i ||
                        boneIndex == JointIndex.thumbMetacarpal.i
    }
}

enum class MirrorType { None, LeftToRight, RightToLeft }