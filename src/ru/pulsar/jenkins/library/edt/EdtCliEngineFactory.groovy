package ru.pulsar.jenkins.library.edt

import java.lang.module.ModuleDescriptor

class EdtCliEngineFactory {

    static getEngine(String edtVersion) {

        def currentVersion = ModuleDescriptor.Version.parse(edtVersion)
        def version2024 = ModuleDescriptor.Version.parse("2024")

        if (currentVersion < version2024) {
            return new RingConverter()
        } else {
            return new NativeEdtCliConverter()
        }
    }
}
