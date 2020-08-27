#
# This Nix expression centralizes the configuration
# for the Android development environment.
#

{ stdenv, config, callPackage, androidenv, openjdk, mkShell }:

androidenv.composeAndroidPackages {
  toolsVersion = "26.1.1";
  platformToolsVersion = "30.0.4";
  buildToolsVersions = [ "30.0.2" ];
  includeEmulator = false;
  platformVersions = [ "29" ];
  includeSources = false;
  includeDocs = false;
  includeSystemImages = false;
  systemImageTypes = [ "default" ];
  cmakeVersions = [ "3.10.2" ];
  includeNDK = true;
  ndkVersion = "21.3.6528147";
  useGoogleAPIs = false;
  useGoogleTVAddOns = false;
  includeExtras = [
    "extras;android;m2repository"
    "extras;google;m2repository"
  ];
  # The "android-sdk-license" license is accepted
  # by setting android_sdk.accept_license = true.
  extraLicenses = [];
}
