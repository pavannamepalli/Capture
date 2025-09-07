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

## 🎮 Usage

### Basic Operation
1. **Launch the app** and grant necessary permissions
2. **Position your hand** within the gesture detection area (indicated by overlay)
3. **Perform gestures** to control the camera:
   - Show open palm to capture photos
   - Make peace sign to start/stop video recording
   - Thumbs up to switch cameras
   - OK sign to open gallery
   - Pinch gestures to zoom in/out
   - Three fingers up to toggle flash

### Gesture Tips
- Ensure good lighting for better gesture recognition
- Keep your hand within the detection area
- Hold gestures steady for 1-2 seconds for reliable detection
- Avoid rapid gesture changes to prevent false triggers

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

