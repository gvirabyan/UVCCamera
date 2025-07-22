#/*
# * UVCCamera
# * library and sample to access to UVC web camera on non-rooted Android device
# *
# * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
# *
# * File name: Application.mk
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# * http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# *
# * All files in the folder are under this Apache License, Version 2.0.
# * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
#*/

# Мы устанавливаем APP_PLATFORM на android-21, так как NDK 27 больше не поддерживает android-14.
# Это также соответствует минимальной версии API, которую мы установили в build.gradle.
APP_PLATFORM := android-21

# Добавляем флаги -fPIC для всех компиляций C и C++.
# Это необходимо для создания динамических библиотек (.so),
# и это решит ошибки "recompile with -fPIC".
APP_CFLAGS += -fPIC
APP_CXXFLAGS += -fPIC

# Выбираем ABI. Оставим все, если нужны x86/x86_64 для эмуляторов,
# иначе можно ограничиться arm64-v8a и armeabi-v7a для уменьшения размера APK.

# Режим оптимизации: debug для отладки, release для финальной сборки.
APP_OPTIM := release

APP_ABI := armeabi-v7a arm64-v8a

# NDK_TOOLCHAIN_VERSION := 4.9 - эту строку можно оставить закомментированной,
# так как NDK уже не поддерживает GCC.