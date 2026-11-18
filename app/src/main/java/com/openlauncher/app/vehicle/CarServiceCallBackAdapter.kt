package com.openlauncher.app.vehicle

import android.os.Bundle
import com.tw.carinfoservice.CarServiceCallBack

/*
 * GENERATED from app/src/main/aidl/com/tw/carinfoservice/CarServiceCallBack.aidl (pulled unchanged
 * from com.tw.carinfoservice's APK). Every callback is a no-op; subclasses override what they use.
 * The service calls onExtendedInterface(null) before each callback; it is ignored here.
 * Regenerate rather than hand-edit if the AIDL changes.
 */
open class CarServiceCallBackAdapter : CarServiceCallBack.Stub() {
    override fun onElectricVoltage(mVoltage: Float, mUnit: String?) {}
    override fun onHeadlight(mHeadlight: Boolean) {}
    override fun onDrivingTime(mDrivingTime: Int) {}
    override fun onTotal_Mileage(mTotal_Mileage: Float, mUnit: String?) {}
    override fun onRecharge_Mileage(mRecharge_Mileage: Float, mUnit: String?) {}
    override fun onAverage_Fuel_Consumption(mAverage_Fuel_Consumption: Float, mUnit: String?) {}
    override fun onInstant_Fuel_Consumption(mInstant_Fuel_Consumption: Float, mUnit: String?) {}
    override fun onGear_Information(mGear_Information: Int) {}
    override fun onMain_Driving_Seat_Belt(mMain_Driving_Seat_Belt: Boolean) {}
    override fun onCo_Pilot_Seat_Belt(mCo_Pilot_Seat_Belt: Boolean) {}
    override fun onHandbrake(mHandbrake: Boolean) {}
    override fun onLeft_Turn_Signal(mLeft_Turn_Signal: Boolean) {}
    override fun onRight_Turn_Signal(mRight_Turn_Signal: Boolean) {}
    override fun onDouble_flash(mDouble_flash: Boolean) {}
    override fun onInstantaneous_Speed(mInstantaneous_Speed: Int) {}
    override fun onEngine_Speed(mEngine_Speed: Int) {}
    override fun onWater_Temp(mWater_Temp: Int, mUnit: String?) {}
    override fun onAmbient_Temp(mAmbient_Temp: Float, mUnit: String?) {}
    override fun onEngine_Oil_Temp(mEngine_Oil_Temp: Float, mUnit: String?) {}
    override fun onOil_Volume(mOil_Volume: Float, mUnit: String?) {}
    override fun onElectricity(mElectricity: Float) {}
    override fun onCharging(mCharging: Int) {}
    override fun onVoltageWarning(mVoltageWarning: Boolean) {}
    override fun onCarReverse(mCarReverse: Boolean) {}
    override fun onLeftFrontDoor(mLeftFrontDoor: Boolean) {}
    override fun onRightFrontDoor(mRightFrontDoor: Boolean) {}
    override fun onBackDoor(mBackDoor: Boolean) {}
    override fun onExpand(bundle: Bundle?) {}
    override fun onOriginalCarVoltage(mVoltage: Float, mUnit: String?) {}
    override fun onTransTemp(mTransTemp: Float, mUnit: String?) {}
    override fun onIntkeTemp(mIntkeTemp: Float, mUnit: String?) {}
    override fun onIntkePressure(mIntkePressure: Float, mUnit: String?) {}
    override fun onTurbocharged(mTurbocharged: Float, mUnit: String?) {}
    override fun onDrivingMode(mDrivingMode: Int) {}
    override fun onOffSign(mOffSign: Boolean) {}
    override fun onOnSign(mOnSign: Boolean) {}
    override fun onLFTirePressure(mLFTirePres: Float, mUnit: String?) {}
    override fun onRFTirePressure(mRFTirePres: Float, mUnit: String?) {}
    override fun onLRTirePressure(mLRTirePres: Float, mUnit: String?) {}
    override fun onRRTirePressure(mRRTirePres: Float, mUnit: String?) {}
    override fun onThrottle(mThrottle: Float, mUnit: String?) {}
    override fun onSteeringWheelAngle(mteeringWheelAngle: Float, mUnit: String?) {}
    override fun onEngineLoad(mEngineLoad: Float, mUnit: String?) {}
    override fun onFRAngle(mFRAngle: Float, mUnit: String?) {}
    override fun onLRAngle(mLRAngle: Float, mUnit: String?) {}
    override fun onMaintenanceMileage(mMaintenanceMileage: Float, mUnit: String?) {}
    override fun onMaintenanceTime(mMaintenanceTime: String?) {}
    override fun onOilWarning(mOilWarning: Boolean) {}
    override fun onDrivenDistance(mDrivenDistance: Int, mUnit: String?) {}
    override fun onLFTireWarning(mTireWarningType: Int, mTireWarningValue: Int) {}
    override fun onRFTireWarning(mTireWarningType: Int, mTireWarningValue: Int) {}
    override fun onLRTireWarning(mTireWarningType: Int, mTireWarningValue: Int) {}
    override fun onRRTireWarning(mTireWarningType: Int, mTireWarningValue: Int) {}
    override fun onLFTireTemp(mTireTemp: Int, mUnit: String?) {}
    override fun onRFTireTemp(mTireTemp: Int, mUnit: String?) {}
    override fun onLRTireTemp(mTireTemp: Int, mUnit: String?) {}
    override fun onRRTireTemp(mTireTemp: Int, mUnit: String?) {}
    override fun onWashingFluidWarning(mWashingFluidWarning: Boolean) {}
    override fun onRadar(mFLR: Int, mFCLR: Int, mFCRR: Int, mFRR: Int, mRightFR: Int, mRightCFR: Int, mRightCRR: Int, mRightRR: Int, mRRR: Int, mRCRR: Int, mRCLR: Int, mRLR: Int, mLeftRR: Int, mLeftCRR: Int, mLeftCFR: Int, mLeftFR: Int) {}
    override fun onTrack(mTrack: Int) {}
    override fun onTurn_Signal(mTurn_Signal: Int) {}
    override fun onAverage_Fuel_Consumption_For_This_Drive(mAverage_Fuel_Consumption: Float, mUnit: String?) {}
    override fun onExtendedInterface(bundle: Bundle?) {}
    override fun onMileage_For_This_Drive(mMileage: Float, mUnit: String?) {}
    override fun onLeftRearDoor(mLeftFrontDoor: Boolean) {}
    override fun onRightRearDoor(mRightFrontDoor: Boolean) {}
}
