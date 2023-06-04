package mahomaps.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import mahomaps.MahoMapsApp;
import mahomaps.Settings;

public class APIReconnectForm extends Form implements Runnable, CommandListener, ItemCommandListener {

	public APIReconnectForm() {
		super("MahoMaps v1");
		setCommandListener(this);
		if (MahoMapsApp.api.token == null) {
			StartTokenRefresh();
		} else {
			ShowOk();
		}
	}

	public void run() {
		Thread.yield();
		try {
			MahoMapsApp.api.RefreshToken();
			ShowSuc();
		} catch (Exception e) {
			ShowFail(e);
		}

	}

	private void StartTokenRefresh() {
		removeCommand(MahoMapsApp.back);
		deleteAll();
		append(new Gauge("Подключение", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		(new Thread(this, "API reconnect")).start();
	}

	private void ShowOk() {
		deleteAll();
		append(new StringItem("Статус", "Подключено"));
		StringItem b = new StringItem("Токен сессии", "Сбросить", Item.BUTTON);
		b.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_LEFT);
		b.setDefaultCommand(MahoMapsApp.reset);
		b.setItemCommandListener(this);
		append(b);
		addCommand(MahoMapsApp.back);
	}

	private void ShowSuc() {
		deleteAll();
		append(new StringItem("Статус", "Подключение успешно. "));
		addCommand(MahoMapsApp.back);
	}

	private void ShowFail(Exception e) {
		deleteAll();
		if (e instanceof SecurityException) {
			append(new StringItem("Статус", "Приложению запрещён доступ в интернет."));
		} else {
			append(new StringItem("Статус", "Ошибка. Попытайтесь ещё раз."));
			append(new StringItem("Прокси", Settings.proxyApi ? "Включено" : "Выключено"));
			append(new StringItem("Возникшее исключение", e.getClass().getName()));
			append(new StringItem("Сведения", e.getMessage()));
		}
		addCommand(MahoMapsApp.back);
	}

	public void commandAction(Command c, Displayable d) {
		if (c == MahoMapsApp.back) {
			MahoMapsApp.BringMenu();
		}
	}

	public void commandAction(Command c, Item item) {
		if (c == MahoMapsApp.reset) {
			MahoMapsApp.api.token = null;
			MahoMapsApp.api.Save();
			StartTokenRefresh();
		}
	}

}
