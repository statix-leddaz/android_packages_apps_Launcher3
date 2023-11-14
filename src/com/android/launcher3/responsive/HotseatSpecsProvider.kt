/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.responsive

import android.content.res.TypedArray
import android.util.Log
import com.android.launcher3.R
import com.android.launcher3.responsive.ResponsiveSpec.Companion.ResponsiveSpecType
import com.android.launcher3.responsive.ResponsiveSpec.DimensionType
import com.android.launcher3.util.ResourceHelper

class HotseatSpecsProvider(private val groupOfSpecs: List<ResponsiveSpecGroup<HotseatSpec>>) {
    fun getSpecsByAspectRatio(aspectRatio: Float): ResponsiveSpecGroup<HotseatSpec> {
        check(aspectRatio > 0f) { "Invalid aspect ratio! The value should be bigger than 0." }

        val specsGroup = groupOfSpecs.firstOrNull { aspectRatio <= it.aspectRatio }
        check(specsGroup != null) { "No available spec with aspectRatio within $aspectRatio." }

        return specsGroup
    }

    fun getCalculatedSpec(
        aspectRatio: Float,
        dimensionType: DimensionType,
        availableSpace: Int
    ): CalculatedHotseatSpec {
        val specsGroup = getSpecsByAspectRatio(aspectRatio)
        val spec = specsGroup.getSpec(dimensionType, availableSpace)
        return CalculatedHotseatSpec(availableSpace, spec)
    }

    companion object {
        @JvmStatic
        fun create(resourceHelper: ResourceHelper): HotseatSpecsProvider {
            val parser = ResponsiveSpecsParser(resourceHelper)
            val specs = parser.parseXML(ResponsiveSpecType.Hotseat, ::HotseatSpec)
            return HotseatSpecsProvider(specs)
        }
    }
}

data class HotseatSpec(
    override val maxAvailableSize: Int,
    override val dimensionType: DimensionType,
    override val specType: ResponsiveSpecType,
    val hotseatQsbSpace: SizeSpec,
    val edgePadding: SizeSpec
) : IResponsiveSpec {
    init {
        check(isValid()) { "Invalid HotseatSpec found." }
    }

    constructor(
        responsiveSpecType: ResponsiveSpecType,
        attrs: TypedArray,
        specs: Map<String, SizeSpec>
    ) : this(
        maxAvailableSize =
            attrs.getDimensionPixelSize(R.styleable.ResponsiveSpec_maxAvailableSize, 0),
        dimensionType =
            DimensionType.entries[
                    attrs.getInt(
                        R.styleable.ResponsiveSpec_dimensionType,
                        DimensionType.HEIGHT.ordinal
                    )],
        specType = responsiveSpecType,
        hotseatQsbSpace = specs.getOrError(SizeSpec.XmlTags.HOTSEAT_QSB_SPACE),
        edgePadding = specs.getOrError(SizeSpec.XmlTags.EDGE_PADDING)
    )

    fun isValid(): Boolean {
        if (maxAvailableSize <= 0) {
            Log.e(LOG_TAG, "${this::class.simpleName}#isValid - maxAvailableSize <= 0")
            return false
        }

        // All specs need to be individually valid
        if (!allSpecsAreValid()) {
            Log.e(LOG_TAG, "${this::class.simpleName}#isValid - !allSpecsAreValid()")
            return false
        }

        return true
    }

    private fun allSpecsAreValid(): Boolean {
        return hotseatQsbSpace.isValid() &&
            hotseatQsbSpace.onlyFixedSize() &&
            edgePadding.isValid() &&
            edgePadding.onlyFixedSize()
    }

    companion object {
        private const val LOG_TAG = "HotseatSpec"
    }
}

class CalculatedHotseatSpec(val availableSpace: Int, val spec: HotseatSpec) {

    var hotseatQsbSpace: Int = 0
        private set

    var edgePadding: Int = 0
        private set

    init {
        hotseatQsbSpace = spec.hotseatQsbSpace.getCalculatedValue(availableSpace)
        edgePadding = spec.edgePadding.getCalculatedValue(availableSpace)
    }

    override fun hashCode(): Int {
        var result = availableSpace.hashCode()
        result = 31 * result + hotseatQsbSpace.hashCode()
        result = 31 * result + edgePadding.hashCode()
        result = 31 * result + spec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is CalculatedHotseatSpec &&
            availableSpace == other.availableSpace &&
            hotseatQsbSpace == other.hotseatQsbSpace &&
            edgePadding == other.edgePadding &&
            spec == other.spec
    }

    override fun toString(): String {
        return "${this::class.simpleName}(" +
            "availableSpace=$availableSpace, hotseatQsbSpace=$hotseatQsbSpace, " +
            "edgePadding=$edgePadding, " +
            "${spec::class.simpleName}.maxAvailableSize=${spec.maxAvailableSize}" +
            ")"
    }
}
