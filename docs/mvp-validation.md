# MVP 검증 기록

검증일: 2026-05-18

## 결론

VoiceGuide MVP는 실제 YOLO26n TFLite 모델 기준으로 카메라 프리뷰, 객체 감지, 박스/라벨 오버레이, 성능 오버레이, 음성/문구 안내까지 동작을 확인했습니다.

## 모델

- 원본: 공식 Ultralytics `yolo26n.pt`
- export 환경: `voiceguide-export`
- 앱 asset: `app/src/main/assets/obstacle_detector.tflite`
- 입력 크기: `320 x 320`
- 출력 형태: `[1, 300, 6]`

## 최종 기기 검증

실기기 `R5CRA1DK9FM`에 debug APK를 설치하고 실제 TFLite detector 모드로 실행했습니다.

확인된 화면 값:

```text
analysis: 2.00 FPS
YUV->NV21: 11 ms
input: 117 ms
inference: 55 ms
postprocess: 0 ms
total: 174 ms
detections: 2
```

positive case:

```text
PERSON 49%
PERSON 29%
정면 가까이에 사람이 있습니다.
```

증거 이미지:

```text
mvp-yolo26n-final.png
```

## 통과한 검증

```text
:app:testDebugUnitTest
:app:assembleDebug -Pvoiceguide.useTfliteDetector=true
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.voiceguide/.MainActivity
adb logcat -d -b crash
python -m pip check
```

## 남은 리스크

MVP 기능 검증은 통과했지만, 장시간 사용 발열은 별도 지속 테스트가 필요합니다. 현재 CPU TFLite 추론 기준 2 FPS에서 동작하며, 발열이 크면 1 FPS 또는 thermal 기반 자동 감속을 다음 단계로 넣는 것이 좋습니다.
