# Android Gesture Camera

A sophisticated Android application that enables hands-free camera control through real-time hand gesture recognition using MediaPipe and CameraX.

## 🎯 Overview

This project implements an advanced gesture-controlled camera system that allows users to capture photos, record videos, and control camera settings using intuitive hand gestures. The application leverages cutting-edge machine learning technologies to provide seamless, touch-free camera operation.

## 🏗️ Architecture

The application follows a **Clean Architecture** pattern with **MVVM (Model-View-ViewModel)** design principles, ensuring maintainability, testability, and scalability.

### Core Architecture Flow
```
CameraX/Camera2 → FrameProcessor → MediaPipe ML → GestureHandler → UIUpdater → CameraController
```

### Architecture Components

#### 1. **Presentation Layer**
- **Activities**: `GestureCameraActivity`, `MainActivity`
- **Fragments**: `CameraFragment`, `GalleryFragment`, `PermissionsFragment`, `GestureInfoFragment`
- **ViewModels**: `GestureCameraViewModel`, `MainViewModel`
- **Views**: `GestureOverlayView`, `OverlayView`

#### 2. **Domain Layer**
- **Use Cases**: `ProcessGestureUseCase`
- **Repositories**: `CameraRepository`, `GestureRepository`
- **Entities**: `GestureType`, `GestureResult`, `CameraAction`

#### 3. **Data Layer**
- **Data Sources**: `CameraDataSourceImpl`, `GestureDataSourceImpl`, `FileSystemDataSource`
- **Models**: `CameraState`, `GestureState`

#### 4. **Core Layer**
- **ML Components**: `GestureDetector`, `HandLandmarkerHelper`, `HandLandmarkUtils`
- **Error Handling**: `AppError` with centralized error management
- **Performance**: `PerformanceMonitor`

#### 5. **Architecture Components**
- **FrameProcessor**: Handles camera frame processing and optimization
- **GestureHandler**: Manages ML model integration and gesture detection
- **UIUpdater**: Updates UI state based on gesture results
- **CameraController**: Controls camera operations triggered by gestures

## 📱 Features

### Gesture Recognition
The application supports **7 distinct hand gestures**:

| Gesture | Action | Description |
|---------|--------|-------------|
| ✋ **Open Palm** | Capture Photo | All fingers extended |
| ✌️ **Peace Sign** | Start/Stop Video Recording | Index and middle finger up |
| 👍 **Thumbs Up** | Switch Camera | Thumb extended, other fingers closed |
| 👌 **OK Sign** | Open Gallery | Thumb and index finger forming circle |
| 🤏 **Pinch Zoom In** | Zoom In | Fingers moving apart |
| 🤏 **Pinch Zoom Out** | Zoom Out | Fingers moving together |
| 🖐️ **Three Fingers Up** | Toggle Flash | Index, thumb, and pinky extended |

### Camera Features
- **Real-time Gesture Detection**: Continuous hand tracking and gesture recognition
- **Photo Capture**: High-quality image capture with gesture control
- **Video Recording**: Start/stop recording with gesture commands
- **Camera Switching**: Toggle between front and rear cameras
- **Zoom Control**: Pinch-to-zoom functionality
- **Flash Control**: Toggle flash on/off
- **Gallery Integration**: Direct access to captured media

### Performance Optimizations
- **Frame Skipping**: Intelligent frame processing to maintain performance
- **Gesture Stability**: Prevents false positives with gesture validation
- **Cooldown Management**: Prevents rapid gesture triggering
- **Background Processing**: ML inference on background threads
- **Memory Management**: Efficient bitmap handling and cleanup

## 🛠️ Technologies & Libraries

### Core Android
- **Kotlin**: Primary programming language
- **Android SDK**: Target SDK 36, Min SDK 24
- **View Binding**: Type-safe view references
- **Navigation Component**: Fragment navigation

### Camera & Media
- **CameraX**: Modern camera API for consistent behavior
- **Camera2**: Advanced camera control
- **MediaStore**: Gallery integration and file management

### Machine Learning
- **MediaPipe**: Google's ML framework for hand landmark detection
- **Hand Landmarker**: Real-time hand tracking and gesture recognition

### Architecture & Concurrency
- **MVVM Pattern**: Clean separation of concerns
- **StateFlow & LiveData**: Reactive UI updates
- **Coroutines**: Asynchronous programming
- **Repository Pattern**: Data abstraction layer

### UI & Design
- **Material Design**: Modern UI components
- **ConstraintLayout**: Flexible layout system
- **Glide**: Image loading and caching
- **Custom Views**: Gesture overlay and feedback

## 📁 Project Structure

```
app/src/main/java/com/example/gesture/
├── architecture/           # Core architecture components
│   ├── FrameProcessor.kt
│   ├── GestureHandler.kt
│   └── UIUpdater.kt
├── camera/                 # Camera control layer
│   └── CameraController.kt
├── core/                   # Core utilities and ML
│   ├── error/             # Error handling
│   └── ml/                # Machine learning components
├── data/                   # Data layer
│   ├── datasource/        # Data sources
│   └── repository/        # Repository implementations
├── domain/                 # Domain layer
│   └── usecase/           # Business logic
├── performance/            # Performance monitoring
├── presentation/           # UI layer
│   ├── activity/          # Activities
│   ├── fragment/          # Fragments
│   ├── view/              # Custom views
│   └── viewmodel/         # ViewModels
└── res/                    # Resources
    ├── layout/            # XML layouts
    ├── values/            # Strings, colors, dimensions
    └── drawable/          # Icons and graphics
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Kotlin 1.8+
- Physical Android device (recommended for camera testing)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd gesture-camera
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the project folder
   - Sync Gradle files

3. **Configure MediaPipe**
   - The MediaPipe dependencies are already configured
   - Ensure you have the latest version of the MediaPipe Tasks Vision library

4. **Run the application**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio
   - Grant camera and microphone permissions when prompted

### Permissions Required
- `CAMERA`: For camera access
- `RECORD_AUDIO`: For video recording
- `WRITE_EXTERNAL_STORAGE`: For saving media (Android 9 and below)
- `READ_EXTERNAL_STORAGE`: For gallery access (Android 10 and below)
- `QUERY_ALL_PACKAGES`: For gallery app detection (Android 11+)

## 🎮 How to Use This App

### Getting Started

#### 1. **First Launch**
- Open the app and grant **Camera** and **Microphone** permissions when prompted
- The app will start with the camera view and a gesture detection overlay
- You'll see a white rectangular box in the center - this is your **gesture detection area**

#### 2. **Understanding the Interface**
- **Camera Preview**: Live camera feed in the background
- **Detection Box**: White rectangular overlay where you perform gestures
- **Recording Indicator**: Red dot with "REC" text when video is recording
- **Gesture Feedback**: Messages appear at the bottom of the screen
- **Toast Notifications**: Action confirmations appear as popup messages

### Gesture Controls

#### 📸 **Photo Capture**
- **Gesture**: ✋ **Open Palm** (all fingers extended)
- **Action**: Captures a photo and saves it to gallery
- **Feedback**: Toast shows "Photo captured and saved to memory"
- **Cooldown**: 3-second countdown, then "Try other gesture" message

#### 🎥 **Video Recording**
- **Start Recording**: ✌️ **Peace Sign** (index and middle finger up)
  - Toast shows "Video recording started"
  - Red recording indicator appears
  - 1-second cooldown, then "You can stop video recording" message
- **Stop Recording**: ✌️ **Peace Sign** again
  - Toast shows "Video recording stopped and video saved to memory"
  - Recording indicator disappears
  - 2-second cooldown, then "Try other gesture" message

#### 📷 **Camera Switching**
- **Gesture**: 👍 **Thumbs Up** (thumb extended, other fingers closed)
- **Action**: Switches between front and rear cameras
- **Feedback**: Toast shows "Camera switched"
- **Cooldown**: 3-second countdown, then "Try other gesture" message

#### 🖼️ **Gallery Access**
- **Gesture**: 👌 **OK Sign** (thumb and index finger forming circle)
- **Action**: Opens the device's gallery app
- **Feedback**: Toast shows "Gallery opened"
- **Cooldown**: 3-second countdown, then "Try other gesture" message

#### 🔍 **Zoom Control**
- **Zoom In**: 🤏 **Pinch Zoom In** (fingers moving apart)
  - Toast shows "Zoomed in"
  - 3-second cooldown, then "Try other gesture" message
- **Zoom Out**: 🤏 **Pinch Zoom Out** (fingers moving together)
  - Toast shows "Zoomed out"
  - 3-second cooldown, then "Try other gesture" message

#### 💡 **Flash Control**
- **Gesture**: 🤟 **Three Fingers Up** (thumb, index, and pinky extended)
- **Back Camera**: Toggles flash on/off
  - Toast shows "Flash toggled"
  - 2-second cooldown, then "Try other gesture" message
- **Front Camera**: Shows "Flash won't work on front camera" toast
  - No cooldown (informational message only)

### Understanding Feedback Messages

#### **Toast Messages** (Popup notifications)
- ✅ **Success Actions**: "Photo captured", "Video recording started", etc.
- ℹ️ **Information**: "Flash won't work on front camera"
- ⚠️ **Errors**: "Error: [error details]"

#### **Overlay Messages** (Bottom of screen)
- **Countdown Timers**: "Try gesture in 3s", "Try gesture in 2s", "Try gesture in 1s"
- **Cooldown Complete**: "Try other gesture" (for most actions)
- **Video Recording**: "You can stop video recording" (after video start cooldown)
- **Blocked Actions**: "First stop video recording \n then proceed to other gesture"

### Step-by-Step Usage Examples

#### **Taking a Photo**
1. Point camera at your subject
2. Position your hand within the white detection box
3. Extend all fingers (open palm gesture)
4. Hold for 1-2 seconds until you see "Photo captured" toast
5. Wait for countdown to complete before taking another photo

#### **Recording a Video**
1. Point camera at your subject
2. Make peace sign (✌️) within the detection box
3. Wait for "Video recording started" toast and red recording indicator
4. When finished, make peace sign (✌️) again
5. Wait for "Video recording stopped" toast

#### **Switching Cameras**
1. Make thumbs up (👍) gesture within the detection box
2. Wait for "Camera switched" toast
3. Camera will switch between front and rear
4. Wait for countdown before switching again

### Best Practices

#### **For Better Recognition**
- **Lighting**: Ensure good, even lighting on your hand
- **Position**: Keep your hand centered in the white detection box
- **Stability**: Hold gestures steady for 1-2 seconds
- **Distance**: Keep hand 1-2 feet from camera for optimal detection
- **Background**: Use solid, contrasting backgrounds when possible

#### **Avoiding Issues**
- **Don't rush**: Wait for countdown to complete before next gesture
- **One gesture at a time**: Don't try multiple gestures simultaneously
- **Clear gestures**: Make sure your gesture is clearly visible and distinct
- **No rapid changes**: Avoid switching between gestures too quickly

#### **During Video Recording**
- **Limited controls**: Only peace sign works to stop recording
- **Other gestures blocked**: You'll see "First stop video recording" message
- **Stop first**: Always stop video before using other gestures

### Troubleshooting Common Issues

#### **Gesture Not Recognized**
- Check if hand is within the white detection box
- Ensure good lighting conditions
- Make sure gesture is clear and distinct
- Wait for any active countdown to complete

#### **App Not Responding**
- Restart the app
- Check camera permissions
- Ensure no other apps are using the camera
- Try switching between front and rear cameras

#### **Poor Performance**
- Close other running applications
- Ensure device has sufficient battery
- Try in better lighting conditions
- Restart the device if issues persist

### Advanced Tips

#### **Gesture Timing**
- **Cooldown periods** prevent accidental multiple triggers
- **Dynamic countdown** shows remaining time
- **"Try other gesture"** appears when ready for next action
- **Video recording** has special timing rules

#### **Camera Features**
- **Front camera**: Flash control shows informational message
- **Back camera**: Full flash control available
- **Zoom**: Works on both cameras
- **Gallery**: Opens system gallery app

#### **Performance Optimization**
- **Frame skipping**: App automatically optimizes for performance
- **Background processing**: ML inference runs on background threads
- **Memory management**: Efficient handling of camera frames
- **Battery optimization**: Minimal impact on device battery

## 🔧 Configuration

### Gesture Sensitivity
The application allows fine-tuning of gesture detection parameters:

- **Detection Confidence**: Minimum confidence threshold for gesture recognition
- **Hand Tracking Confidence**: Accuracy of hand landmark detection
- **Hand Presence Confidence**: Threshold for hand visibility
- **Max Hands**: Maximum number of hands to detect simultaneously

### Performance Settings
- **Frame Processing**: Configurable frame skipping for performance optimization
- **Gesture Cooldown**: Prevents rapid gesture triggering
- **ML Model**: Choose between CPU, GPU, or NNAPI for inference

## 🧪 Testing

### Manual Testing
1. **Gesture Recognition**: Test each gesture type individually
2. **Camera Functions**: Verify photo capture, video recording, and camera switching
3. **Performance**: Monitor frame rates and memory usage
4. **Edge Cases**: Test with poor lighting, multiple hands, and rapid movements

### Performance Monitoring
The app includes built-in performance monitoring:
- Frame processing statistics
- Gesture detection accuracy
- Memory usage tracking
- Inference time measurements

## 🐛 Troubleshooting

### Common Issues

**Gesture Not Detected**
- Ensure good lighting conditions
- Check if hand is within detection area
- Verify gesture is performed correctly
- Try adjusting sensitivity settings

**Camera Issues**
- Grant camera permissions
- Restart the app
- Check device camera functionality
- Ensure no other apps are using the camera

**Performance Issues**
- Close other running applications
- Reduce gesture detection sensitivity
- Enable frame skipping in settings
- Use CPU inference instead of GPU

## 📊 Performance Metrics

The application provides real-time performance monitoring:

- **Frame Rate**: 30 FPS target with adaptive frame skipping
- **Inference Time**: <100ms average for gesture detection
- **Memory Usage**: Optimized bitmap handling and cleanup
- **Battery Impact**: Efficient background processing

## 🔮 Future Enhancements

### Planned Features
- **Additional Gestures**: More gesture types for extended functionality
- **Voice Commands**: Integration with speech recognition
- **Cloud Processing**: Optional cloud-based gesture recognition
- **Custom Gestures**: User-defined gesture training
- **Multi-language Support**: Internationalization
- **Accessibility**: Enhanced accessibility features

### Technical Improvements
- **Model Optimization**: Smaller, faster ML models
- **Edge Computing**: Improved on-device processing
- **Real-time Analytics**: Advanced performance metrics
- **A/B Testing**: Gesture recognition algorithm testing

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on how to:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📞 Support

For support, questions, or feature requests:

- **Issues**: Create an issue on GitHub
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check the wiki for detailed guides

## 🙏 Acknowledgments

- **Google MediaPipe Team**: For the excellent ML framework
- **Android CameraX Team**: For the modern camera API
- **TensorFlow Team**: For the machine learning infrastructure
- **Open Source Community**: For various libraries and tools used

---

