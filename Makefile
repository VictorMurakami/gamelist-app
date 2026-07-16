.PHONY: setup run-android run-ios test lint build-debug build-release clean

setup:
	./gradlew --version
	@echo "✓ Gradle OK"
	@echo "Run 'make run-android' or 'make run-ios' to start the app"

run-android:
	./gradlew :composeApp:installDebug

run-ios:
	cd iosApp && xcodebuild -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16' build

test:
	./gradlew :composeApp:allTests

test-android:
	./gradlew :composeApp:testDebugUnitTest

lint:
	./gradlew :composeApp:lintDebug

build-debug:
	./gradlew :composeApp:assembleDebug

build-release:
	./gradlew :composeApp:assembleRelease

clean:
	./gradlew clean
