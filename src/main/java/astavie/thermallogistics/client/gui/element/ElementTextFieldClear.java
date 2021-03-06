package astavie.thermallogistics.client.gui.element;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.client.gui.IFocusGui;
import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.ElementTextField;

public class ElementTextFieldClear extends ElementTextField {

	private boolean rightClick = false;
	private boolean permanent;

	public ElementTextFieldClear(GuiContainerCore gui, int posX, int posY, int width, int height, boolean permanent) {
		super(gui, posX, posY, width, height);
		this.permanent = permanent;
	}

	@Override
	public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) {
		if (mouseButton == 1) {
			rightClick = true;
			this.setText("");
			this.setFocused(true);
			return true;
		}
		return super.onMousePressed(mouseX, mouseY, mouseButton);
	}

	public void onMouseReleased(int mouseX, int mouseY) {
		if (this.rightClick) {
			this.rightClick = false;
		} else if (!permanent || !ThermalLogistics.INSTANCE.autofocus.getBoolean()) {
			super.onMouseReleased(mouseX, mouseY);
		}
	}

	@Override
	public ElementTextField setFocused(boolean focused) {
		boolean prev = isFocused();

		super.setFocused(focused);

		if (prev == focused)
			return this;

		if (gui instanceof IFocusGui) {
			if (focused)
				((IFocusGui) gui).onFocus(this);
			else
				((IFocusGui) gui).onLeave(this);
		}

		return this;
	}

}
