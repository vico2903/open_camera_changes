package net.sourceforge.opencamera.camera2

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import net.sourceforge.opencamera.camera2.model.Camera2ApiProperties
import net.sourceforge.opencamera.camera2.model.CameraModel
import net.sourceforge.opencamera.camera2.model.DerivedProperties
import java.util.function.Consumer

class CameraFinder(cameraManager: CameraManager) :
    CameraFinderAbstract<ArrayList<CameraModel>>(cameraManager) {
    init {
        cameraModels = ArrayList()
    }

    public override fun createModels() {
        validCameraIds.forEach(Consumer { cameraModels.add(CameraModel(it.toInt())) })
        cameraModels.forEach(Consumer {
            it.camera2ApiProperties = findProperties(it.id, getCameraCharacteristics(it.id)!!)
        })
        cameraModels.forEach(Consumer {
            it.derivedProperties = deriveProperties(it.id, it.camera2ApiProperties)
        })
    }

    public override fun findProperties(
        cameraId: Int, characteristics: CameraCharacteristics): Camera2ApiProperties {
        return Camera2ApiProperties(cameraId).apply {
            facing = characteristics.get(CameraCharacteristics.LENS_FACING)!!
            focalLength =
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)!![0]
            aperture =
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)!![0]
            aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)!!
            isFlashSupported = characteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE)!!
            val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (configMap?.getOutputSizes(ImageFormat.RAW_SENSOR) != null) {
                rawSensorSizes = configMap.getOutputSizes(ImageFormat.RAW_SENSOR)
            }
            sensorSize = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
            pixelArraySize = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)!!
            supportedHardwareLevel = characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
            supportedHardwareLevelString = reflectionProvider.getResultFieldName(
                CameraMetadata::class.java,
                "INFO_SUPPORTED_HARDWARE_LEVEL_",
                supportedHardwareLevel
            )
            physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                characteristics.physicalCameraIds
            } else {
                setOf()
            }
        }
    }

    public override fun deriveProperties(
        cameraId: Int,
        camera2ApiProperties: Camera2ApiProperties
    ): DerivedProperties {
        return DerivedProperties(cameraId).apply {
            facing = reflectionProvider.getResultFieldName(
                CameraMetadata::class.java,
                "LENS_FACING_",
                camera2ApiProperties.facing
            )
            isLogical = camera2ApiProperties.physicalIds.isNotEmpty()
            angleOfView = CameraUtil.calculateAngleOfView(
                camera2ApiProperties.focalLength,
                camera2ApiProperties.sensorSize,
                camera2ApiProperties.pixelArraySize
            )
            pixelSize = CameraUtil.calculatePixelSize(
                camera2ApiProperties.pixelArraySize.width,
                camera2ApiProperties.sensorSize.width
            )
            mm35FocalLength =
                CameraUtil.calculate35mmeqv(
                    camera2ApiProperties.focalLength,
                    camera2ApiProperties.sensorSize
                )
        }
    }
}
