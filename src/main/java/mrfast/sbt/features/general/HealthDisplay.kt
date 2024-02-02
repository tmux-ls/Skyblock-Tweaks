package mrfast.sbt.features.general

import mrfast.sbt.apis.PlayerStats
import mrfast.sbt.config.Categories.GeneralConfig
import mrfast.sbt.config.GuiManager
import mrfast.sbt.utils.GuiUtils
import mrfast.sbt.utils.LocationUtils
import mrfast.sbt.utils.Utils
import mrfast.sbt.utils.Utils.formatNumber

object HealthDisplay {
    init {
        HealthDisplayGui()
    }

    class HealthDisplayGui : GuiManager.Element() {
        init {
            this.relativeX = 0.371875
            this.relativeY = 0.842593
            this.elementName = "Health Display"
            this.addToList()
            this.height = Utils.mc.fontRendererObj.FONT_HEIGHT
        }

        override fun draw() {
            val color = if(PlayerStats.health>PlayerStats.maxHealth) "§6" else "§c"
            val display = "$color${PlayerStats.health.formatNumber()}§c/${PlayerStats.maxHealth.formatNumber()}"
            GuiUtils.drawText(display, 0f, 0f, GuiUtils.TextStyle.BLACK_OUTLINE)
            this.width = Utils.mc.fontRendererObj.getStringWidth(display)+1
        }

        override fun isActive(): Boolean {
            return GeneralConfig.healthDisplay && LocationUtils.inSkyblock
        }

        override fun isVisible(): Boolean {
            return true
        }
    }
}