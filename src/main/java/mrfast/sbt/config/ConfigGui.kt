package mrfast.sbt.config

import gg.essential.api.utils.GuiUtil
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.constraint
import gg.essential.universal.UMatrixStack
import gg.essential.vigilance.gui.settings.ColorComponent
import gg.essential.vigilance.gui.settings.SelectorComponent
import mrfast.sbt.SkyblockTweaks
import mrfast.sbt.config.Categories.CustomizationConfig
import mrfast.sbt.config.Components.LPosChildSizeConstraint
import mrfast.sbt.config.Components.NumberInputComponent
import mrfast.sbt.config.Components.TextInputComponent
import mrfast.sbt.config.Components.ToggleSwitchComponent
import mrfast.sbt.utils.ChatUtils
import mrfast.sbt.utils.Utils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.client.config.GuiUtils
import org.apache.logging.log4j.LogManager
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*


class ConfigGui : WindowScreen(ElementaVersion.V2, newGuiScale = 2) {
    companion object {
        var listeningForKeybind = false
        var searchQuery = ""

        fun openConfigSearch(query:String) {
            searchQuery = query
            GuiUtil.open(ConfigGui())
        }
    }

    private var mainBackgroundColor = BasicState(Color(22, 22, 22))
    private var categoriesBackgroundColor = BasicState(Color(28, 28, 28))
    private var headerBackgroundColor = BasicState(Color(34, 34, 34))
    private var guiLineColors = BasicState(Color(130, 130, 130))
    private var mainBorderColor = BasicState(Color(0, 255, 255))
    private var defaultCategoryColor = BasicState(Color(180, 180, 180))
    private var selectedCategoryColor = BasicState(Color(0, 255, 255))
    private var hoveredCategoryColor = BasicState(Color(255, 255, 255))
    private var updateSymbol = BasicState(UIText())

    private var showUpdateButton = true

    private var selectedCategory = "General"
    private var selectedCategoryComponent: UIComponent? = null

    private var tooltipElements: MutableMap<UIComponent, Set<String>> = mutableMapOf()

    override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)
        for (element in tooltipElements.keys) {
            if (element.isHovered()) {
                GuiUtils.drawHoveringText(tooltipElements[element]?.toMutableList()
                        ?: mutableListOf(), mouseX, mouseY, window.getWidth().toInt(), window.getHeight().toInt(), -1, Minecraft.getMinecraft().fontRendererObj)
            }
        }
    }

    override fun onScreenClose() {
        super.onScreenClose()

        SkyblockTweaks.config.saveConfig()
        updateBlinkyTimer.cancel()
        updateBorderTimer.cancel()
    }

    init {
        // Create a background panel
        val background = UIBlock(mainBackgroundColor).constrain {
            width = 1.pixels
            height = 4.pixels
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf window effect OutlineEffect(color = mainBorderColor, BasicState(2f), drawAfterChildren = true, drawInsideChildren = true)

        background.animate {
            setWidthAnimation(Animations.IN_OUT_EXP, 0.25f, MinConstraint(70.percent, 600.pixels))
            setHeightAnimation(Animations.IN_OUT_EXP, 0.35f, MinConstraint(70.percent, 400.pixels), 0.2f)
        }

        animateBorder()

        Inspector(background) childOf window
        // Use 70% width, max 600px

        val header = UIBlock(headerBackgroundColor).constrain {
            width = 100.percent
            height = 30.pixels
            x = 0.pixels()
            y = 0.pixels()
        } childOf background effect OutlineEffect(guiLineColors.get(), 1f, sides = setOf(OutlineEffect.Side.Bottom))

        // Add some text to the panel
        val modTitle = UIText("§eSkyblock §9Tweaks").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf header
        modTitle.setTextScale(2.5.pixels)

        val modVersion = UIText("§7v1.0.0").constrain {
            x = SiblingConstraint(4f)
            y = SiblingConstraint(4f) - 10f.pixels()
        } childOf header

        modVersion.setTextScale(0.75.pixels)

        val searchBar = UIBlock(mainBackgroundColor).constrain {
            width = MinConstraint(12.percent, 100.pixels)
            height = 12.pixels
            x = PixelConstraint(10f, true)
            y = CenterConstraint()
        } childOf header effect OutlineEffect(guiLineColors.get(), 1f)

        val searchBarInput = UITextInput("Search").constrain {
            width = 100.percent
            height = 8.pixels
            x = CenterConstraint() + 1.pixels
            y = CenterConstraint()
        } childOf searchBar
        searchBarInput.setColor(Color.GRAY)

        header.onMouseClick {
            if (!searchBarInput.isActive()) {
                searchBarInput.grabWindowFocus()
            }
        }

        if (showUpdateButton) {
            val updateButton = UIBlock(mainBackgroundColor).constrain {
                width = 16.pixels
                height = 16.pixels
                x = SiblingConstraint(15f, true)
                y = CenterConstraint()
            } childOf header effect OutlineEffect(guiLineColors.get(), 1f)

            val updateSymbolNew = UIText("⬆").constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            } childOf updateButton
            updateSymbolNew.setTextScale(2.pixels)

            animateUpdateButton()
            updateSymbol.set(updateSymbolNew)

            updateButton.onMouseClick {
                // do update command
            }
            updateButton.addTooltip(setOf("§aUpdate §ev1.2.0§a is available! Click to download"))
        }

        val categoryListBackground = UIBlock(categoriesBackgroundColor).constrain {
            x = 0.pixels
            y = 32.pixels
            width = 21.percent
            height = 100.percent - 32.pixels
        } childOf background effect OutlineEffect(guiLineColors.get(), 1f, sides = setOf(OutlineEffect.Side.Right), drawInsideChildren = true) effect ScissorEffect()

        val categoryList = ScrollComponent("").constrain {
            x = 0.pixels
            y = 8.pixels
            width = 100.percent
            height = 100.percent
        } childOf categoryListBackground

        val scrollbar = UIRoundedRectangle(3f).constrain {
            width = 5.pixels
            height = 100.percent
            x = PixelConstraint(0f, alignOpposite = true)
            color = Color(200, 200, 200, 200).constraint
        } childOf categoryListBackground

        categoryList.setVerticalScrollBarComponent(scrollbar, true)

        val featureListBackground = UIContainer().constrain {
            x = 21.percent + 1.pixels
            y = 40.pixels
            width = 79.percent - 3.pixels
            height = 100.percent - 40.pixels
        } childOf background effect ScissorEffect()
        val featureList = ScrollComponent("No features by that name found :(").constrain {
            x = 21.percent + 2.pixels
            y = 32.pixels
            width = 79.percent - 12.pixels
            height = 100.percent - 34.pixels
        } childOf background

        val featureScrollbar = UIRoundedRectangle(3f).constrain {
            width = 5.pixels
            height = 100.percent
            x = PixelConstraint(0f, alignOpposite = true)
            color = Color(200, 200, 200, 200).constraint
        } childOf featureListBackground

        featureList.setVerticalScrollBarComponent(featureScrollbar, true)

        searchBarInput.onUpdate { text ->
            searchQuery = searchBarInput.getText()
            updateSelectedFeatures(featureList)
        }

        if(searchQuery.isNotEmpty()) {
            Utils.setTimeout({
                searchBarInput.setText(searchQuery)
                ChatUtils.logMessage("$searchQuery SEARCH QUERY")
            },200)
        }

        for ((count, category) in ConfigManager.categories.values.withIndex()) {
            val actualY = if (count == 0) 10.pixels else SiblingConstraint(3f)

            val categoryComponent = UIText(category.name).constrain {
                x = CenterConstraint()
                y = actualY
                height = 8.pixels
                color = defaultCategoryColor.constraint
            } childOf categoryList
            if (selectedCategory == category.name) {
                updateSelectedCategoryColor(categoryComponent, category.name)
                updateSelectedFeatures(featureList)
            }
            categoryComponent.setTextScale(1.6.pixels)

            categoryComponent.onMouseEnter {
                // Dont do hover colors if already colored
                if (selectedCategory != category.name) {
                    if (!categoryComponent.getColor().equals(hoveredCategoryColor.constraint)) {
                        categoryComponent.animate {
                            setColorAnimation(Animations.OUT_EXP, 0.2f, hoveredCategoryColor.constraint)
                        }
                    }
                }
            }
            categoryComponent.onMouseLeave {
                // Dont do hover colors if already colored
                if (selectedCategory != category.name) {
                    if (!categoryComponent.getColor().equals(defaultCategoryColor.constraint)) {
                        categoryComponent.animate {
                            setColorAnimation(Animations.OUT_EXP, 0.2f, defaultCategoryColor.constraint)
                        }
                    }
                }
            }

            categoryComponent.onMouseClick {
                updateSelectedCategoryColor(categoryComponent, category.name)
                updateSelectedFeatures(featureList)
                searchQuery = ""
                SkyblockTweaks.config.saveConfig()
            }
        }
    }

    private var updateBlinkyTimer = Timer()

    private fun animateUpdateButton() {
        val colors = listOf(Color(85, 255, 85), Color(16, 100, 16))
        var currentColorIndex = 0
        updateBlinkyTimer = Timer()
        updateBlinkyTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // blinky arrow
                updateSymbol.get().animate {
                    setColorAnimation(Animations.OUT_EXP, 0.5f, colors[currentColorIndex].constraint)
                }

                // Toggle to the next color index
                currentColorIndex = (currentColorIndex + 1) % colors.size
            }
        }, 0, 500)
    }


    private var updateBorderTimer = Timer()
    private var hueCounter = 0.0
    private fun animateBorder() {
        updateBorderTimer = Timer()
        updateBorderTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (!CustomizationConfig.chromaConfigBorder) {
                    mainBorderColor.set(Color(0, 255, 255))
                    return
                }
                hueCounter += 5
                val hue = hueCounter % 360
                val outlineColor = Color.HSBtoRGB(hue.toFloat() / 360f, 1f, 1f)
                mainBorderColor.set(Color(outlineColor))
            }
        }, 0, 100)
    }


    private fun updateSelectedCategoryColor(new: UIComponent, name: String) {
        selectedCategoryComponent?.animate {
            setColorAnimation(Animations.OUT_EXP, 0.2f, defaultCategoryColor.constraint)
        }
        new.animate {
            setColorAnimation(Animations.OUT_EXP, 0.2f, selectedCategoryColor.constraint)
        }
        selectedCategoryComponent = new
        selectedCategory = name
    }

    private fun updateSelectedFeatures(list: ScrollComponent) {
        list.clearChildren()

        var drawnCategories = 0
        for (category in ConfigManager.categories) {
            // If searching something, ignore selected category
            if (category.key != selectedCategory && searchQuery.isEmpty()) continue

            for (subcategory in category.value.subcategories.values) {
                val actualY = if (drawnCategories == 0) 12.pixels else SiblingConstraint(6f)
                var drawnFeatures = 0

                val subcategoryComponent = UIContainer().constrain {
                    height = ChildBasedSizeConstraint() + 20.pixels
                    width = 100.percent
                    x = 0.pixels
                    y = actualY
                }

                val subcategoryTitle = UIText(subcategory.name).constrain {
                    x = CenterConstraint()
                    y = 0.pixels
                    textScale = 1.8.pixels
                } childOf subcategoryComponent

                for (feature in subcategory.features.values) {
                    // Check if name,description,subcategory contain the search

                    if (!(containsIgnoreCase(feature.name, searchQuery) ||
                                    containsIgnoreCase(feature.description, searchQuery) ||
                                    containsIgnoreCase(subcategory.name, searchQuery))) {
                        continue
                    }

                    val featureBackground = UIBlock(headerBackgroundColor).constrain {
                        x = CenterConstraint()
                        y = SiblingConstraint(6f)
                        width = 95.percent
                        height = ChildBasedSizeConstraint(2f)
                    } childOf subcategoryComponent effect OutlineEffect(guiLineColors.get(), 1f)

                    val featureTitle = UIText(feature.name).constrain {
                        x = 2.pixels
                        y = 2.pixels
                        textScale = 1.5.pixels
                    } childOf featureBackground

                    val featureDescription = UIWrappedText(feature.description).constrain {
                        x = 2.pixels
                        y = SiblingConstraint(2f)
                        width = 80.percent - 2.pixels
                        color = Color.GRAY.constraint
                    } childOf featureBackground

                    populateFeature(feature, featureBackground)
                    drawnFeatures++
                }
                // Dont draw subcategory title if no features
                if (drawnFeatures != 0) {
                    subcategoryComponent childOf list
                }
                drawnCategories++
            }
        }
    }

    private fun containsIgnoreCase(source: String, target: String): Boolean {
        return source.lowercase(Locale.getDefault()).contains(target.lowercase(Locale.getDefault()))
    }

    private fun populateFeature(feature: ConfigManager.Feature, featureComponent: UIComponent) {
        val ignoredHeights = mutableListOf<UIComponent>()

        if (feature.type == ConfigType.TOGGLE) {
            val toggleSwitch = ToggleSwitchComponent(feature.value as Boolean).constrain {
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent
            toggleSwitch.onMouseClick {
                feature.field.set(SkyblockTweaks.config, toggleSwitch.activated)
            }
            ignoredHeights.add(toggleSwitch)
        }

        if (feature.type == ConfigType.NUMBER) {
            val numberInput = NumberInputComponent(feature.value as Int).constrain {
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent
            numberInput.onKeyType { typedChar, keyCode ->
                feature.field.set(SkyblockTweaks.config, numberInput.intValue)
            }
            ignoredHeights.add(numberInput)
        }

        if (feature.type == ConfigType.TEXT) {
            val textInput = TextInputComponent(feature.value as String).constrain {
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent

            textInput.onKeyType { typedChar, keyCode ->
                feature.field.set(SkyblockTweaks.config, textInput.text)
            }
            ignoredHeights.add(textInput)

        }

        if (feature.type == ConfigType.COLOR) {
            val color = feature.value as Color
            val colorPicker = ColorComponent(color, false).constrain {
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent

            val colorDisplay = UIBlock(color.constraint).constrain {
                width = 16.pixels
                height = 16.pixels
                y = CenterConstraint()
                x = SiblingConstraint(3f, true)
            } childOf featureComponent

            val resetImg = UIImage.ofResource("/skyblocktweaks/gui/reset.png").constrain {
                width = 10.pixels
                height = 11.pixels
                y = CenterConstraint()
                x = SiblingConstraint(3f, true)
            } childOf featureComponent

            resetImg.onMouseClick {
                val defaultValue = ConfigManager.defaultMap[feature.field.name] ?: Color.GRAY

                feature.field.set(SkyblockTweaks.config, defaultValue)
                colorDisplay.setColor(defaultValue as Color)
            }

            colorPicker.onValueChange { value: Any? ->
                colorDisplay.setColor(value as Color)
                feature.field.set(SkyblockTweaks.config, value)
            }

            ignoredHeights.addAll(mutableListOf(colorDisplay, resetImg, featureComponent.children[1]))
        }

        if (feature.type == ConfigType.DROPDOWN) {
            var selected = feature.dropdownOptions.indexOf(feature.value)
            if (selected == -1) selected = 0
            val selector = SelectorComponent(selected, feature.dropdownOptions.toList()).constrain {
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent
            selector.onValueChange { value: Any? ->
                feature.field.set(SkyblockTweaks.config, feature.dropdownOptions[value as Int])
            }
        }
        if (feature.type == ConfigType.KEYBIND) {
            val button = UIImage.ofResource("/skyblocktweaks/gui/button.png").constrain {
                width = 88.pixels
                height = 24.pixels
                y = CenterConstraint()
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent
            ignoredHeights.add(button)

            val resetImg = UIImage.ofResource("/skyblocktweaks/gui/reset.png").constrain {
                width = 10.pixels
                height = 11.pixels
                y = CenterConstraint()
                x = SiblingConstraint(3f, true)
            } childOf featureComponent
            ignoredHeights.add(resetImg)

            val keycode = feature.value as Int
            var currentKey = "NONE"
            if (keycode != -1) {
                currentKey = Keyboard.getKeyName(keycode)
            }
            val buttonText = UIText(currentKey).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            } childOf button

            button.onMouseClickConsumer {
                if (listeningForKeybind) return@onMouseClickConsumer
                listeningForKeybind = true
                // Set listening style, similar to minecrafts keybind system
                buttonText.setText("§r> §e" + buttonText.getText() + "§r <")

                Thread {
                    var keyPressed = false
                    while (!keyPressed && listeningForKeybind) {
                        for (i in 0 until Keyboard.KEYBOARD_SIZE) {
                            if (Keyboard.isKeyDown(i)) {
                                Utils.setTimeout({
                                    listeningForKeybind = false
                                }, 100)

                                val newKeyName = Keyboard.getKeyName(i)

                                // Reset if ESCAPE is pressed
                                if (i == 1) {
                                    buttonText.setText("NONE")
                                    listeningForKeybind = false
                                    break
                                }
                                buttonText.setText(newKeyName)
                                keyPressed = true
                                feature.field.set(SkyblockTweaks.config, i)

                                break
                            }
                        }

                        // Add a small delay to avoid excessive CPU usage
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }.start()
            }

            resetImg.onMouseClick {
                val defaultValue = ConfigManager.defaultMap[feature.field.name]
                var newCurrentKey: String? = "NONE"
                if (defaultValue != -1) newCurrentKey = Keyboard.getKeyName(defaultValue as Int)

                listeningForKeybind = false
                buttonText.setText(newCurrentKey!!)
                feature.field.set(SkyblockTweaks.config, defaultValue)
            }
        }
        if (feature.type == ConfigType.BUTTON) {
            val button = UIImage.ofResource("/skyblocktweaks/gui/button.png").constrain {
                width = 88.pixels
                height = 24.pixels
                y = CenterConstraint()
                x = 10.pixels(alignOpposite = true)
            } childOf featureComponent

            val buttonText = UIText(feature.placeholder).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            } childOf button

            button.onMouseClick {
                (feature.value as Runnable).run()
            }
            ignoredHeights.add(button)
        }

        // Stop the setting options from effecting total height
        featureComponent.setHeight(LPosChildSizeConstraint(ignoredHeights) + 5.pixels)

        if (feature.isParent) {
            val unhovered = ConstantColorConstraint(Color(200, 200, 200))
            val hovered = ConstantColorConstraint(Color(255, 255, 255))

            val settingsGear = UIImage.ofResourceCached("/assets/skyblocktweaks/gui/gear.png")
                    .setX(PixelConstraint(50f, true))
                    .setY(CenterConstraint())
                    .setHeight(PixelConstraint(16f))
                    .setColor(unhovered)
                    .setWidth(PixelConstraint(16f))
                    .setChildOf(featureComponent)

            settingsGear.onMouseEnterRunnable {
                val anim = settingsGear.makeAnimation()
                anim.setColorAnimation(Animations.OUT_EXP, 0.5f, hovered)
                settingsGear.animateTo(anim)
            }
            settingsGear.onMouseLeaveRunnable {
                val anim = settingsGear.makeAnimation()
                anim.setColorAnimation(Animations.OUT_EXP, 0.5f, unhovered)
                settingsGear.animateTo(anim)
            }
        }
    }

    private fun UIComponent.addTooltip(set: Set<String>) {
        tooltipElements[this] = set
    }
}