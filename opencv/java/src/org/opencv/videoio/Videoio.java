//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.videoio;

import java.util.ArrayList;
import java.util.List;
import org.opencv.utils.Converters;

// C++: class Videoio

public class Videoio {

    // C++: enum <unnamed>
    public static final int
            CAP_PROP_DC1394_OFF = -4,
            CAP_PROP_DC1394_MODE_MANUAL = -3,
            CAP_PROP_DC1394_MODE_AUTO = -2,
            CAP_PROP_DC1394_MODE_ONE_PUSH_AUTO = -1,
            CAP_PROP_DC1394_MAX = 31,
            CAP_OPENNI_DEPTH_GENERATOR = 1 << 31,
            CAP_OPENNI_IMAGE_GENERATOR = 1 << 30,
            CAP_OPENNI_IR_GENERATOR = 1 << 29,
            CAP_OPENNI_GENERATORS_MASK = CAP_OPENNI_DEPTH_GENERATOR + CAP_OPENNI_IMAGE_GENERATOR + CAP_OPENNI_IR_GENERATOR,
            CAP_PROP_OPENNI_OUTPUT_MODE = 100,
            CAP_PROP_OPENNI_FRAME_MAX_DEPTH = 101,
            CAP_PROP_OPENNI_BASELINE = 102,
            CAP_PROP_OPENNI_FOCAL_LENGTH = 103,
            CAP_PROP_OPENNI_REGISTRATION = 104,
            CAP_PROP_OPENNI_REGISTRATION_ON = CAP_PROP_OPENNI_REGISTRATION,
            CAP_PROP_OPENNI_APPROX_FRAME_SYNC = 105,
            CAP_PROP_OPENNI_MAX_BUFFER_SIZE = 106,
            CAP_PROP_OPENNI_CIRCLE_BUFFER = 107,
            CAP_PROP_OPENNI_MAX_TIME_DURATION = 108,
            CAP_PROP_OPENNI_GENERATOR_PRESENT = 109,
            CAP_PROP_OPENNI2_SYNC = 110,
            CAP_PROP_OPENNI2_MIRROR = 111,
            CAP_OPENNI_IMAGE_GENERATOR_PRESENT = +CAP_OPENNI_IMAGE_GENERATOR + CAP_PROP_OPENNI_GENERATOR_PRESENT,
            CAP_OPENNI_IMAGE_GENERATOR_OUTPUT_MODE = +CAP_OPENNI_IMAGE_GENERATOR + CAP_PROP_OPENNI_OUTPUT_MODE,
            CAP_OPENNI_DEPTH_GENERATOR_PRESENT = +CAP_OPENNI_DEPTH_GENERATOR + CAP_PROP_OPENNI_GENERATOR_PRESENT,
            CAP_OPENNI_DEPTH_GENERATOR_BASELINE = +CAP_OPENNI_DEPTH_GENERATOR + CAP_PROP_OPENNI_BASELINE,
            CAP_OPENNI_DEPTH_GENERATOR_FOCAL_LENGTH = +CAP_OPENNI_DEPTH_GENERATOR + CAP_PROP_OPENNI_FOCAL_LENGTH,
            CAP_OPENNI_DEPTH_GENERATOR_REGISTRATION = +CAP_OPENNI_DEPTH_GENERATOR + CAP_PROP_OPENNI_REGISTRATION,
            CAP_OPENNI_DEPTH_GENERATOR_REGISTRATION_ON = CAP_OPENNI_DEPTH_GENERATOR_REGISTRATION,
            CAP_OPENNI_IR_GENERATOR_PRESENT = +CAP_OPENNI_IR_GENERATOR + CAP_PROP_OPENNI_GENERATOR_PRESENT,
            CAP_OPENNI_DEPTH_MAP = 0,
            CAP_OPENNI_POINT_CLOUD_MAP = 1,
            CAP_OPENNI_DISPARITY_MAP = 2,
            CAP_OPENNI_DISPARITY_MAP_32F = 3,
            CAP_OPENNI_VALID_DEPTH_MASK = 4,
            CAP_OPENNI_BGR_IMAGE = 5,
            CAP_OPENNI_GRAY_IMAGE = 6,
            CAP_OPENNI_IR_IMAGE = 7,
            CAP_OPENNI_VGA_30HZ = 0,
            CAP_OPENNI_SXGA_15HZ = 1,
            CAP_OPENNI_SXGA_30HZ = 2,
            CAP_OPENNI_QVGA_30HZ = 3,
            CAP_OPENNI_QVGA_60HZ = 4,
            CAP_PROP_GSTREAMER_QUEUE_LENGTH = 200,
            CAP_PROP_PVAPI_MULTICASTIP = 300,
            CAP_PROP_PVAPI_FRAMESTARTTRIGGERMODE = 301,
            CAP_PROP_PVAPI_DECIMATIONHORIZONTAL = 302,
            CAP_PROP_PVAPI_DECIMATIONVERTICAL = 303,
            CAP_PROP_PVAPI_BINNINGX = 304,
            CAP_PROP_PVAPI_BINNINGY = 305,
            CAP_PROP_PVAPI_PIXELFORMAT = 306,
            CAP_PVAPI_FSTRIGMODE_FREERUN = 0,
            CAP_PVAPI_FSTRIGMODE_SYNCIN1 = 1,
            CAP_PVAPI_FSTRIGMODE_SYNCIN2 = 2,
            CAP_PVAPI_FSTRIGMODE_FIXEDRATE = 3,
            CAP_PVAPI_FSTRIGMODE_SOFTWARE = 4,
            CAP_PVAPI_DECIMATION_OFF = 1,
            CAP_PVAPI_DECIMATION_2OUTOF4 = 2,
            CAP_PVAPI_DECIMATION_2OUTOF8 = 4,
            CAP_PVAPI_DECIMATION_2OUTOF16 = 8,
            CAP_PVAPI_PIXELFORMAT_MONO8 = 1,
            CAP_PVAPI_PIXELFORMAT_MONO16 = 2,
            CAP_PVAPI_PIXELFORMAT_BAYER8 = 3,
            CAP_PVAPI_PIXELFORMAT_BAYER16 = 4,
            CAP_PVAPI_PIXELFORMAT_RGB24 = 5,
            CAP_PVAPI_PIXELFORMAT_BGR24 = 6,
            CAP_PVAPI_PIXELFORMAT_RGBA32 = 7,
            CAP_PVAPI_PIXELFORMAT_BGRA32 = 8,
            CAP_PROP_XI_DOWNSAMPLING = 400,
            CAP_PROP_XI_DATA_FORMAT = 401,
            CAP_PROP_XI_OFFSET_X = 402,
            CAP_PROP_XI_OFFSET_Y = 403,
            CAP_PROP_XI_TRG_SOURCE = 404,
            CAP_PROP_XI_TRG_SOFTWARE = 405,
            CAP_PROP_XI_GPI_SELECTOR = 406,
            CAP_PROP_XI_GPI_MODE = 407,
            CAP_PROP_XI_GPI_LEVEL = 408,
            CAP_PROP_XI_GPO_SELECTOR = 409,
            CAP_PROP_XI_GPO_MODE = 410,
            CAP_PROP_XI_LED_SELECTOR = 411,
            CAP_PROP_XI_LED_MODE = 412,
            CAP_PROP_XI_MANUAL_WB = 413,
            CAP_PROP_XI_AUTO_WB = 414,
            CAP_PROP_XI_AEAG = 415,
            CAP_PROP_XI_EXP_PRIORITY = 416,
            CAP_PROP_XI_AE_MAX_LIMIT = 417,
            CAP_PROP_XI_AG_MAX_LIMIT = 418,
            CAP_PROP_XI_AEAG_LEVEL = 419,
            CAP_PROP_XI_TIMEOUT = 420,
            CAP_PROP_XI_EXPOSURE = 421,
            CAP_PROP_XI_EXPOSURE_BURST_COUNT = 422,
            CAP_PROP_XI_GAIN_SELECTOR = 423,
            CAP_PROP_XI_GAIN = 424,
            CAP_PROP_XI_DOWNSAMPLING_TYPE = 426,
            CAP_PROP_XI_BINNING_SELECTOR = 427,
            CAP_PROP_XI_BINNING_VERTICAL = 428,
            CAP_PROP_XI_BINNING_HORIZONTAL = 429,
            CAP_PROP_XI_BINNING_PATTERN = 430,
            CAP_PROP_XI_DECIMATION_SELECTOR = 431,
            CAP_PROP_XI_DECIMATION_VERTICAL = 432,
            CAP_PROP_XI_DECIMATION_HORIZONTAL = 433,
            CAP_PROP_XI_DECIMATION_PATTERN = 434,
            CAP_PROP_XI_TEST_PATTERN_GENERATOR_SELECTOR = 587,
            CAP_PROP_XI_TEST_PATTERN = 588,
            CAP_PROP_XI_IMAGE_DATA_FORMAT = 435,
            CAP_PROP_XI_SHUTTER_TYPE = 436,
            CAP_PROP_XI_SENSOR_TAPS = 437,
            CAP_PROP_XI_AEAG_ROI_OFFSET_X = 439,
            CAP_PROP_XI_AEAG_ROI_OFFSET_Y = 440,
            CAP_PROP_XI_AEAG_ROI_WIDTH = 441,
            CAP_PROP_XI_AEAG_ROI_HEIGHT = 442,
            CAP_PROP_XI_BPC = 445,
            CAP_PROP_XI_WB_KR = 448,
            CAP_PROP_XI_WB_KG = 449,
            CAP_PROP_XI_WB_KB = 450,
            CAP_PROP_XI_WIDTH = 451,
            CAP_PROP_XI_HEIGHT = 452,
            CAP_PROP_XI_REGION_SELECTOR = 589,
            CAP_PROP_XI_REGION_MODE = 595,
            CAP_PROP_XI_LIMIT_BANDWIDTH = 459,
            CAP_PROP_XI_SENSOR_DATA_BIT_DEPTH = 460,
            CAP_PROP_XI_OUTPUT_DATA_BIT_DEPTH = 461,
            CAP_PROP_XI_IMAGE_DATA_BIT_DEPTH = 462,
            CAP_PROP_XI_OUTPUT_DATA_PACKING = 463,
            CAP_PROP_XI_OUTPUT_DATA_PACKING_TYPE = 464,
            CAP_PROP_XI_IS_COOLED = 465,
            CAP_PROP_XI_COOLING = 466,
            CAP_PROP_XI_TARGET_TEMP = 467,
            CAP_PROP_XI_CHIP_TEMP = 468,
            CAP_PROP_XI_HOUS_TEMP = 469,
            CAP_PROP_XI_HOUS_BACK_SIDE_TEMP = 590,
            CAP_PROP_XI_SENSOR_BOARD_TEMP = 596,
            CAP_PROP_XI_CMS = 470,
            CAP_PROP_XI_APPLY_CMS = 471,
            CAP_PROP_XI_IMAGE_IS_COLOR = 474,
            CAP_PROP_XI_COLOR_FILTER_ARRAY = 475,
            CAP_PROP_XI_GAMMAY = 476,
            CAP_PROP_XI_GAMMAC = 477,
            CAP_PROP_XI_SHARPNESS = 478,
            CAP_PROP_XI_CC_MATRIX_00 = 479,
            CAP_PROP_XI_CC_MATRIX_01 = 480,
            CAP_PROP_XI_CC_MATRIX_02 = 481,
            CAP_PROP_XI_CC_MATRIX_03 = 482,
            CAP_PROP_XI_CC_MATRIX_10 = 483,
            CAP_PROP_XI_CC_MATRIX_11 = 484,
            CAP_PROP_XI_CC_MATRIX_12 = 485,
            CAP_PROP_XI_CC_MATRIX_13 = 486,
            CAP_PROP_XI_CC_MATRIX_20 = 487,
            CAP_PROP_XI_CC_MATRIX_21 = 488,
            CAP_PROP_XI_CC_MATRIX_22 = 489,
            CAP_PROP_XI_CC_MATRIX_23 = 490,
            CAP_PROP_XI_CC_MATRIX_30 = 491,
            CAP_PROP_XI_CC_MATRIX_31 = 492,
            CAP_PROP_XI_CC_MATRIX_32 = 493,
            CAP_PROP_XI_CC_MATRIX_33 = 494,
            CAP_PROP_XI_DEFAULT_CC_MATRIX = 495,
            CAP_PROP_XI_TRG_SELECTOR = 498,
            CAP_PROP_XI_ACQ_FRAME_BURST_COUNT = 499,
            CAP_PROP_XI_DEBOUNCE_EN = 507,
            CAP_PROP_XI_DEBOUNCE_T0 = 508,
            CAP_PROP_XI_DEBOUNCE_T1 = 509,
            CAP_PROP_XI_DEBOUNCE_POL = 510,
            CAP_PROP_XI_LENS_MODE = 511,
            CAP_PROP_XI_LENS_APERTURE_VALUE = 512,
            CAP_PROP_XI_LENS_FOCUS_MOVEMENT_VALUE = 513,
            CAP_PROP_XI_LENS_FOCUS_MOVE = 514,
            CAP_PROP_XI_LENS_FOCUS_DISTANCE = 515,
            CAP_PROP_XI_LENS_FOCAL_LENGTH = 516,
            CAP_PROP_XI_LENS_FEATURE_SELECTOR = 517,
            CAP_PROP_XI_LENS_FEATURE = 518,
            CAP_PROP_XI_DEVICE_MODEL_ID = 521,
            CAP_PROP_XI_DEVICE_SN = 522,
            CAP_PROP_XI_IMAGE_DATA_FORMAT_RGB32_ALPHA = 529,
            CAP_PROP_XI_IMAGE_PAYLOAD_SIZE = 530,
            CAP_PROP_XI_TRANSPORT_PIXEL_FORMAT = 531,
            CAP_PROP_XI_SENSOR_CLOCK_FREQ_HZ = 532,
            CAP_PROP_XI_SENSOR_CLOCK_FREQ_INDEX = 533,
            CAP_PROP_XI_SENSOR_OUTPUT_CHANNEL_COUNT = 534,
            CAP_PROP_XI_FRAMERATE = 535,
            CAP_PROP_XI_COUNTER_SELECTOR = 536,
            CAP_PROP_XI_COUNTER_VALUE = 537,
            CAP_PROP_XI_ACQ_TIMING_MODE = 538,
            CAP_PROP_XI_AVAILABLE_BANDWIDTH = 539,
            CAP_PROP_XI_BUFFER_POLICY = 540,
            CAP_PROP_XI_LUT_EN = 541,
            CAP_PROP_XI_LUT_INDEX = 542,
            CAP_PROP_XI_LUT_VALUE = 543,
            CAP_PROP_XI_TRG_DELAY = 544,
            CAP_PROP_XI_TS_RST_MODE = 545,
            CAP_PROP_XI_TS_RST_SOURCE = 546,
            CAP_PROP_XI_IS_DEVICE_EXIST = 547,
            CAP_PROP_XI_ACQ_BUFFER_SIZE = 548,
            CAP_PROP_XI_ACQ_BUFFER_SIZE_UNIT = 549,
            CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_SIZE = 550,
            CAP_PROP_XI_BUFFERS_QUEUE_SIZE = 551,
            CAP_PROP_XI_ACQ_TRANSPORT_BUFFER_COMMIT = 552,
            CAP_PROP_XI_RECENT_FRAME = 553,
            CAP_PROP_XI_DEVICE_RESET = 554,
            CAP_PROP_XI_COLUMN_FPN_CORRECTION = 555,
            CAP_PROP_XI_ROW_FPN_CORRECTION = 591,
            CAP_PROP_XI_SENSOR_MODE = 558,
            CAP_PROP_XI_HDR = 559,
            CAP_PROP_XI_HDR_KNEEPOINT_COUNT = 560,
            CAP_PROP_XI_HDR_T1 = 561,
            CAP_PROP_XI_HDR_T2 = 562,
            CAP_PROP_XI_KNEEPOINT1 = 563,
            CAP_PROP_XI_KNEEPOINT2 = 564,
            CAP_PROP_XI_IMAGE_BLACK_LEVEL = 565,
            CAP_PROP_XI_HW_REVISION = 571,
            CAP_PROP_XI_DEBUG_LEVEL = 572,
            CAP_PROP_XI_AUTO_BANDWIDTH_CALCULATION = 573,
            CAP_PROP_XI_FFS_FILE_ID = 594,
            CAP_PROP_XI_FFS_FILE_SIZE = 580,
            CAP_PROP_XI_FREE_FFS_SIZE = 581,
            CAP_PROP_XI_USED_FFS_SIZE = 582,
            CAP_PROP_XI_FFS_ACCESS_KEY = 583,
            CAP_PROP_XI_SENSOR_FEATURE_SELECTOR = 585,
            CAP_PROP_XI_SENSOR_FEATURE_VALUE = 586,
            CAP_PROP_ARAVIS_AUTOTRIGGER = 600,
            CAP_PROP_ANDROID_DEVICE_TORCH = 8001,
            CAP_PROP_IOS_DEVICE_FOCUS = 9001,
            CAP_PROP_IOS_DEVICE_EXPOSURE = 9002,
            CAP_PROP_IOS_DEVICE_FLASH = 9003,
            CAP_PROP_IOS_DEVICE_WHITEBALANCE = 9004,
            CAP_PROP_IOS_DEVICE_TORCH = 9005,
            CAP_PROP_GIGA_FRAME_OFFSET_X = 10001,
            CAP_PROP_GIGA_FRAME_OFFSET_Y = 10002,
            CAP_PROP_GIGA_FRAME_WIDTH_MAX = 10003,
            CAP_PROP_GIGA_FRAME_HEIGH_MAX = 10004,
            CAP_PROP_GIGA_FRAME_SENS_WIDTH = 10005,
            CAP_PROP_GIGA_FRAME_SENS_HEIGH = 10006,
            CAP_PROP_INTELPERC_PROFILE_COUNT = 11001,
            CAP_PROP_INTELPERC_PROFILE_IDX = 11002,
            CAP_PROP_INTELPERC_DEPTH_LOW_CONFIDENCE_VALUE = 11003,
            CAP_PROP_INTELPERC_DEPTH_SATURATION_VALUE = 11004,
            CAP_PROP_INTELPERC_DEPTH_CONFIDENCE_THRESHOLD = 11005,
            CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_HORZ = 11006,
            CAP_PROP_INTELPERC_DEPTH_FOCAL_LENGTH_VERT = 11007,
            CAP_INTELPERC_DEPTH_GENERATOR = 1 << 29,
            CAP_INTELPERC_IMAGE_GENERATOR = 1 << 28,
            CAP_INTELPERC_IR_GENERATOR = 1 << 27,
            CAP_INTELPERC_GENERATORS_MASK = CAP_INTELPERC_DEPTH_GENERATOR + CAP_INTELPERC_IMAGE_GENERATOR + CAP_INTELPERC_IR_GENERATOR,
            CAP_INTELPERC_DEPTH_MAP = 0,
            CAP_INTELPERC_UVDEPTH_MAP = 1,
            CAP_INTELPERC_IR_MAP = 2,
            CAP_INTELPERC_IMAGE = 3,
            CAP_PROP_GPHOTO2_PREVIEW = 17001,
            CAP_PROP_GPHOTO2_WIDGET_ENUMERATE = 17002,
            CAP_PROP_GPHOTO2_RELOAD_CONFIG = 17003,
            CAP_PROP_GPHOTO2_RELOAD_ON_CHANGE = 17004,
            CAP_PROP_GPHOTO2_COLLECT_MSGS = 17005,
            CAP_PROP_GPHOTO2_FLUSH_MSGS = 17006,
            CAP_PROP_SPEED = 17007,
            CAP_PROP_APERTURE = 17008,
            CAP_PROP_EXPOSUREPROGRAM = 17009,
            CAP_PROP_VIEWFINDER = 17010,
            CAP_PROP_IMAGES_BASE = 18000,
            CAP_PROP_IMAGES_LAST = 19000;


    // C++: enum VideoAccelerationType (cv.VideoAccelerationType)
    public static final int
            VIDEO_ACCELERATION_NONE = 0,
            VIDEO_ACCELERATION_ANY = 1,
            VIDEO_ACCELERATION_D3D11 = 2,
            VIDEO_ACCELERATION_VAAPI = 3,
            VIDEO_ACCELERATION_MFX = 4;


    // C++: enum VideoCaptureAPIs (cv.VideoCaptureAPIs)
    public static final int
            CAP_ANY = 0,
            CAP_VFW = 200,
            CAP_V4L = 200,
            CAP_V4L2 = CAP_V4L,
            CAP_FIREWIRE = 300,
            CAP_FIREWARE = CAP_FIREWIRE,
            CAP_IEEE1394 = CAP_FIREWIRE,
            CAP_DC1394 = CAP_FIREWIRE,
            CAP_CMU1394 = CAP_FIREWIRE,
            CAP_QT = 500,
            CAP_UNICAP = 600,
            CAP_DSHOW = 700,
            CAP_PVAPI = 800,
            CAP_OPENNI = 900,
            CAP_OPENNI_ASUS = 910,
            CAP_ANDROID = 1000,
            CAP_XIAPI = 1100,
            CAP_AVFOUNDATION = 1200,
            CAP_GIGANETIX = 1300,
            CAP_MSMF = 1400,
            CAP_WINRT = 1410,
            CAP_INTELPERC = 1500,
            CAP_REALSENSE = 1500,
            CAP_OPENNI2 = 1600,
            CAP_OPENNI2_ASUS = 1610,
            CAP_OPENNI2_ASTRA = 1620,
            CAP_GPHOTO2 = 1700,
            CAP_GSTREAMER = 1800,
            CAP_FFMPEG = 1900,
            CAP_IMAGES = 2000,
            CAP_ARAVIS = 2100,
            CAP_OPENCV_MJPEG = 2200,
            CAP_INTEL_MFX = 2300,
            CAP_XINE = 2400,
            CAP_UEYE = 2500,
            CAP_OBSENSOR = 2600;


    // C++: enum VideoCaptureOBSensorDataType (cv.VideoCaptureOBSensorDataType)
    public static final int
            CAP_OBSENSOR_DEPTH_MAP = 0,
            CAP_OBSENSOR_BGR_IMAGE = 1,
            CAP_OBSENSOR_IR_IMAGE = 2;


    // C++: enum VideoCaptureOBSensorGenerators (cv.VideoCaptureOBSensorGenerators)
    public static final int
            CAP_OBSENSOR_DEPTH_GENERATOR = 1 << 29,
            CAP_OBSENSOR_IMAGE_GENERATOR = 1 << 28,
            CAP_OBSENSOR_IR_GENERATOR = 1 << 27,
            CAP_OBSENSOR_GENERATORS_MASK = CAP_OBSENSOR_DEPTH_GENERATOR + CAP_OBSENSOR_IMAGE_GENERATOR + CAP_OBSENSOR_IR_GENERATOR;


    // C++: enum VideoCaptureOBSensorProperties (cv.VideoCaptureOBSensorProperties)
    public static final int
            CAP_PROP_OBSENSOR_INTRINSIC_FX = 26001,
            CAP_PROP_OBSENSOR_INTRINSIC_FY = 26002,
            CAP_PROP_OBSENSOR_INTRINSIC_CX = 26003,
            CAP_PROP_OBSENSOR_INTRINSIC_CY = 26004;


    // C++: enum VideoCaptureProperties (cv.VideoCaptureProperties)
    public static final int
            CAP_PROP_POS_MSEC = 0,
            CAP_PROP_POS_FRAMES = 1,
            CAP_PROP_POS_AVI_RATIO = 2,
            CAP_PROP_FRAME_WIDTH = 3,
            CAP_PROP_FRAME_HEIGHT = 4,
            CAP_PROP_FPS = 5,
            CAP_PROP_FOURCC = 6,
            CAP_PROP_FRAME_COUNT = 7,
            CAP_PROP_FORMAT = 8,
            CAP_PROP_MODE = 9,
            CAP_PROP_BRIGHTNESS = 10,
            CAP_PROP_CONTRAST = 11,
            CAP_PROP_SATURATION = 12,
            CAP_PROP_HUE = 13,
            CAP_PROP_GAIN = 14,
            CAP_PROP_EXPOSURE = 15,
            CAP_PROP_CONVERT_RGB = 16,
            CAP_PROP_WHITE_BALANCE_BLUE_U = 17,
            CAP_PROP_RECTIFICATION = 18,
            CAP_PROP_MONOCHROME = 19,
            CAP_PROP_SHARPNESS = 20,
            CAP_PROP_AUTO_EXPOSURE = 21,
            CAP_PROP_GAMMA = 22,
            CAP_PROP_TEMPERATURE = 23,
            CAP_PROP_TRIGGER = 24,
            CAP_PROP_TRIGGER_DELAY = 25,
            CAP_PROP_WHITE_BALANCE_RED_V = 26,
            CAP_PROP_ZOOM = 27,
            CAP_PROP_FOCUS = 28,
            CAP_PROP_GUID = 29,
            CAP_PROP_ISO_SPEED = 30,
            CAP_PROP_BACKLIGHT = 32,
            CAP_PROP_PAN = 33,
            CAP_PROP_TILT = 34,
            CAP_PROP_ROLL = 35,
            CAP_PROP_IRIS = 36,
            CAP_PROP_SETTINGS = 37,
            CAP_PROP_BUFFERSIZE = 38,
            CAP_PROP_AUTOFOCUS = 39,
            CAP_PROP_SAR_NUM = 40,
            CAP_PROP_SAR_DEN = 41,
            CAP_PROP_BACKEND = 42,
            CAP_PROP_CHANNEL = 43,
            CAP_PROP_AUTO_WB = 44,
            CAP_PROP_WB_TEMPERATURE = 45,
            CAP_PROP_CODEC_PIXEL_FORMAT = 46,
            CAP_PROP_BITRATE = 47,
            CAP_PROP_ORIENTATION_META = 48,
            CAP_PROP_ORIENTATION_AUTO = 49,
            CAP_PROP_HW_ACCELERATION = 50,
            CAP_PROP_HW_DEVICE = 51,
            CAP_PROP_HW_ACCELERATION_USE_OPENCL = 52,
            CAP_PROP_OPEN_TIMEOUT_MSEC = 53,
            CAP_PROP_READ_TIMEOUT_MSEC = 54,
            CAP_PROP_STREAM_OPEN_TIME_USEC = 55,
            CAP_PROP_VIDEO_TOTAL_CHANNELS = 56,
            CAP_PROP_VIDEO_STREAM = 57,
            CAP_PROP_AUDIO_STREAM = 58,
            CAP_PROP_AUDIO_POS = 59,
            CAP_PROP_AUDIO_SHIFT_NSEC = 60,
            CAP_PROP_AUDIO_DATA_DEPTH = 61,
            CAP_PROP_AUDIO_SAMPLES_PER_SECOND = 62,
            CAP_PROP_AUDIO_BASE_INDEX = 63,
            CAP_PROP_AUDIO_TOTAL_CHANNELS = 64,
            CAP_PROP_AUDIO_TOTAL_STREAMS = 65,
            CAP_PROP_AUDIO_SYNCHRONIZE = 66,
            CAP_PROP_LRF_HAS_KEY_FRAME = 67,
            CAP_PROP_CODEC_EXTRADATA_INDEX = 68,
            CAP_PROP_FRAME_TYPE = 69,
            CAP_PROP_N_THREADS = 70,
            CAP_PROP_PTS = 71,
            CAP_PROP_DTS_DELAY = 72;


    // C++: enum VideoWriterProperties (cv.VideoWriterProperties)
    public static final int
            VIDEOWRITER_PROP_QUALITY = 1,
            VIDEOWRITER_PROP_FRAMEBYTES = 2,
            VIDEOWRITER_PROP_NSTRIPES = 3,
            VIDEOWRITER_PROP_IS_COLOR = 4,
            VIDEOWRITER_PROP_DEPTH = 5,
            VIDEOWRITER_PROP_HW_ACCELERATION = 6,
            VIDEOWRITER_PROP_HW_DEVICE = 7,
            VIDEOWRITER_PROP_HW_ACCELERATION_USE_OPENCL = 8,
            VIDEOWRITER_PROP_RAW_VIDEO = 9,
            VIDEOWRITER_PROP_KEY_INTERVAL = 10,
            VIDEOWRITER_PROP_KEY_FLAG = 11,
            VIDEOWRITER_PROP_PTS = 12,
            VIDEOWRITER_PROP_DTS_DELAY = 13;


    //
    // C++:  String cv::videoio_registry::getBackendName(VideoCaptureAPIs api)
    //

    /**
     * Returns backend API name or "UnknownVideoAPI(xxx)"
     * @param api backend ID (#VideoCaptureAPIs)
     * @return automatically generated
     */
    public static String getBackendName(int api) {
        return getBackendName_0(api);
    }


    //
    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getBackends()
    //

    /**
     * Returns list of all available backends
     * @return automatically generated
     */
    public static List<Integer> getBackends() {
        return getBackends_0();
    }


    //
    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getCameraBackends()
    //

    /**
     * Returns list of available backends which works via {@code cv::VideoCapture(int index)}
     * @return automatically generated
     */
    public static List<Integer> getCameraBackends() {
        return getCameraBackends_0();
    }


    //
    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getStreamBackends()
    //

    /**
     * Returns list of available backends which works via {@code cv::VideoCapture(filename)}
     * @return automatically generated
     */
    public static List<Integer> getStreamBackends() {
        return getStreamBackends_0();
    }


    //
    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getStreamBufferedBackends()
    //

    /**
     * Returns list of available backends which works via {@code cv::VideoCapture(buffer)}
     * @return automatically generated
     */
    public static List<Integer> getStreamBufferedBackends() {
        return getStreamBufferedBackends_0();
    }


    //
    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getWriterBackends()
    //

    /**
     * Returns list of available backends which works via {@code cv::VideoWriter()}
     * @return automatically generated
     */
    public static List<Integer> getWriterBackends() {
        return getWriterBackends_0();
    }


    //
    // C++:  bool cv::videoio_registry::hasBackend(VideoCaptureAPIs api)
    //

    /**
     * Returns true if backend is available
     * @param api automatically generated
     * @return automatically generated
     */
    public static boolean hasBackend(int api) {
        return hasBackend_0(api);
    }


    //
    // C++:  bool cv::videoio_registry::isBackendBuiltIn(VideoCaptureAPIs api)
    //

    /**
     * Returns true if backend is built in (false if backend is used as plugin)
     * @param api automatically generated
     * @return automatically generated
     */
    public static boolean isBackendBuiltIn(int api) {
        return isBackendBuiltIn_0(api);
    }


    //
    // C++:  string cv::videoio_registry::getCameraBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    //

    /**
     * Returns description and ABI/API version of videoio plugin's camera interface
     * @param api automatically generated
     * @param version_ABI automatically generated
     * @param version_API automatically generated
     * @return automatically generated
     */
    public static String getCameraBackendPluginVersion(int api, int[] version_ABI, int[] version_API) {
        double[] version_ABI_out = new double[1];
        double[] version_API_out = new double[1];
        String retVal = getCameraBackendPluginVersion_0(api, version_ABI_out, version_API_out);
        if(version_ABI!=null) version_ABI[0] = (int)version_ABI_out[0];
        if(version_API!=null) version_API[0] = (int)version_API_out[0];
        return retVal;
    }


    //
    // C++:  string cv::videoio_registry::getStreamBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    //

    /**
     * Returns description and ABI/API version of videoio plugin's stream capture interface
     * @param api automatically generated
     * @param version_ABI automatically generated
     * @param version_API automatically generated
     * @return automatically generated
     */
    public static String getStreamBackendPluginVersion(int api, int[] version_ABI, int[] version_API) {
        double[] version_ABI_out = new double[1];
        double[] version_API_out = new double[1];
        String retVal = getStreamBackendPluginVersion_0(api, version_ABI_out, version_API_out);
        if(version_ABI!=null) version_ABI[0] = (int)version_ABI_out[0];
        if(version_API!=null) version_API[0] = (int)version_API_out[0];
        return retVal;
    }


    //
    // C++:  string cv::videoio_registry::getStreamBufferedBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    //

    /**
     * Returns description and ABI/API version of videoio plugin's buffer capture interface
     * @param api automatically generated
     * @param version_ABI automatically generated
     * @param version_API automatically generated
     * @return automatically generated
     */
    public static String getStreamBufferedBackendPluginVersion(int api, int[] version_ABI, int[] version_API) {
        double[] version_ABI_out = new double[1];
        double[] version_API_out = new double[1];
        String retVal = getStreamBufferedBackendPluginVersion_0(api, version_ABI_out, version_API_out);
        if(version_ABI!=null) version_ABI[0] = (int)version_ABI_out[0];
        if(version_API!=null) version_API[0] = (int)version_API_out[0];
        return retVal;
    }


    //
    // C++:  string cv::videoio_registry::getWriterBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    //

    /**
     * Returns description and ABI/API version of videoio plugin's writer interface
     * @param api automatically generated
     * @param version_ABI automatically generated
     * @param version_API automatically generated
     * @return automatically generated
     */
    public static String getWriterBackendPluginVersion(int api, int[] version_ABI, int[] version_API) {
        double[] version_ABI_out = new double[1];
        double[] version_API_out = new double[1];
        String retVal = getWriterBackendPluginVersion_0(api, version_ABI_out, version_API_out);
        if(version_ABI!=null) version_ABI[0] = (int)version_ABI_out[0];
        if(version_API!=null) version_API[0] = (int)version_API_out[0];
        return retVal;
    }




    // C++:  String cv::videoio_registry::getBackendName(VideoCaptureAPIs api)
    private static native String getBackendName_0(int api);

    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getBackends()
    private static native List<Integer> getBackends_0();

    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getCameraBackends()
    private static native List<Integer> getCameraBackends_0();

    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getStreamBackends()
    private static native List<Integer> getStreamBackends_0();

    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getStreamBufferedBackends()
    private static native List<Integer> getStreamBufferedBackends_0();

    // C++:  vector_VideoCaptureAPIs cv::videoio_registry::getWriterBackends()
    private static native List<Integer> getWriterBackends_0();

    // C++:  bool cv::videoio_registry::hasBackend(VideoCaptureAPIs api)
    private static native boolean hasBackend_0(int api);

    // C++:  bool cv::videoio_registry::isBackendBuiltIn(VideoCaptureAPIs api)
    private static native boolean isBackendBuiltIn_0(int api);

    // C++:  string cv::videoio_registry::getCameraBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    private static native String getCameraBackendPluginVersion_0(int api, double[] version_ABI_out, double[] version_API_out);

    // C++:  string cv::videoio_registry::getStreamBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    private static native String getStreamBackendPluginVersion_0(int api, double[] version_ABI_out, double[] version_API_out);

    // C++:  string cv::videoio_registry::getStreamBufferedBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    private static native String getStreamBufferedBackendPluginVersion_0(int api, double[] version_ABI_out, double[] version_API_out);

    // C++:  string cv::videoio_registry::getWriterBackendPluginVersion(VideoCaptureAPIs api, int& version_ABI, int& version_API)
    private static native String getWriterBackendPluginVersion_0(int api, double[] version_ABI_out, double[] version_API_out);

}
