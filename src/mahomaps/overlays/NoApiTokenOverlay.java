package mahomaps.overlays;

import java.util.Vector;

import mahomaps.MahoMapsApp;
import mahomaps.map.Geopoint;
import mahomaps.screens.APIReconnectForm;
import mahomaps.ui.Button;
import mahomaps.ui.ColumnsContainer;
import mahomaps.ui.FillFlowContainer;
import mahomaps.ui.IButtonHandler;
import mahomaps.ui.SimpleText;
import mahomaps.ui.UIElement;

public class NoApiTokenOverlay extends MapOverlay implements IButtonHandler {

	public NoApiTokenOverlay() {
		content = new FillFlowContainer(new UIElement[] { new SimpleText("Не удалось получить токен API."),
				new SimpleText("Онлайн-функции недоступны."), new ColumnsContainer(
						new UIElement[] { new Button("Ещё раз", 1, this), new Button("Закрыть", 0, this) }) });
	}

	public String GetId() {
		return "no_token";
	}

	public Vector GetPoints() {
		return EMPTY_VECTOR;
	}

	public boolean OnPointTap(Geopoint p) {
		return false;
	}

	public void OnButtonTap(UIElement sender, int uid) {
		switch (uid) {
		case 0:
			Close();
			break;
		case 1:
			MahoMapsApp.BringSubScreen(new APIReconnectForm());
			Close();
			break;
		}

	}

}
