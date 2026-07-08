.PHONY: help publish-local publish release clean test check-env

# Extracts the current version from the library's build.gradle
VERSION := $(shell grep "def sdkVersion =" EgoiPushLibrary/build.gradle | cut -d '"' -f 2)

help:
	@echo "========================================================="
	@echo "EgoiPushLibrary Deployment Makefile"
	@echo "Current SDK Version: $(VERSION)"
	@echo "========================================================="
	@echo "Available commands:"
	@echo "  make publish-local   - Publish to local Maven repository (~/.m2)"
	@echo "  make publish         - Upload to Maven Central (manual release required in portal)"
	@echo "  make release         - Upload and automatically release to Maven Central"
	@echo "  make test            - Run unit tests for the library"
	@echo "  make clean           - Clean all build directories"
	@echo "  make check-env       - Verify if publishing credentials are set up"
	@echo "========================================================="

check-env:
	@echo "Verifying publishing credentials in ~/.gradle/gradle.properties..."
	@if ! grep -q "mavenCentralUsername" ~/.gradle/gradle.properties; then echo "Error: mavenCentralUsername missing"; exit 1; fi
	@if ! grep -q "mavenCentralPassword" ~/.gradle/gradle.properties; then echo "Error: mavenCentralPassword missing"; exit 1; fi
	@if ! grep -q "signing.keyId" ~/.gradle/gradle.properties; then echo "Error: signing.keyId missing"; exit 1; fi
	@echo "Credentials verified successfully."

test:
	./gradlew :EgoiPushLibrary:test

publish-local:
	./gradlew :EgoiPushLibrary:publishToMavenLocal

publish: check-env
	./gradlew :EgoiPushLibrary:publishToMavenCentral

release: check-env
	./gradlew :EgoiPushLibrary:publishAndReleaseToMavenCentral

clean:
	./gradlew clean
