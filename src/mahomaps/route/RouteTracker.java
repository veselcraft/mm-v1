package mahomaps.route;

import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import mahomaps.MahoMapsApp;
import mahomaps.map.Geopoint;
import mahomaps.map.MapState;
import mahomaps.overlays.MapOverlay;
import mahomaps.overlays.RouteFollowOverlay;
import mahomaps.screens.MapCanvas;

public class RouteTracker {

	final Image icons;
	public final RouteFollowOverlay overlay;
	Geopoint trueGeolocation = null;
	Geopoint extrapolatedGeolocation = null;
	private final Geopoint[] vertex;
	private final float[] lineLengths;
	private final RouteSegment[] segments;
	int currentSegment;

	static final int ACCEPTABLE_ERROR = 20;

	// drawing temps
	private TrackerOverlayState tos = new TrackerOverlayState(RouteSegment.NO_ICON, 0, "", "Начинаем маршрут...", "");
	private MapCanvas map;

	public RouteTracker(Route r, RouteFollowOverlay o) throws IOException {
		this.overlay = o;
		vertex = r.points;
		segments = r.segments;
		currentSegment = -2;
		lineLengths = new float[vertex.length - 1];
		for (int i = 0; i < lineLengths.length; i++) {
			lineLengths[i] = Distance(vertex[i], vertex[i + 1]);
		}
		icons = Image.createImage("/navigator50.png");
	}

	public void SpoofGeolocation(MapCanvas m) {
		map = m;
		trueGeolocation = map.geolocation;
		extrapolatedGeolocation = new Geopoint(trueGeolocation.lat, trueGeolocation.lon);
		extrapolatedGeolocation.type = Geopoint.LOCATION;
		extrapolatedGeolocation.color = Geopoint.COLOR_RED;
		map.geolocation = extrapolatedGeolocation;
	}

	public void ReleaseGeolocation() {
		map.geolocation = trueGeolocation;
		trueGeolocation = null;
		extrapolatedGeolocation = null;
		map = null;
	}

	/**
	 * Call this every frame to make tracker work.
	 */
	public void Update() {
		extrapolatedGeolocation.lat = trueGeolocation.lat;
		extrapolatedGeolocation.lon = trueGeolocation.lon;
		MapState ms = MapState.FocusAt(extrapolatedGeolocation, map.state.zoom);
		map.state = ms;
		if (currentSegment == -2) {
			// first update
			if (distTo(vertex[0]) < ACCEPTABLE_ERROR) {
				currentSegment = 0;
			} else {
				currentSegment = -1;
			}
		}
		if (currentSegment == -1) {
			// route start is not reached
			float d = distTo(vertex[0]);
			final RouteSegment rs = segments[0];
			tos = new TrackerOverlayState(rs.GetIcon(), getSegmentAngle(rs), "Проследуйте к старту",
					"Осталось " + ((int) d) + "м", rs.GetDescription());
			overlay.ShowPoint(rs.GetAnchor());
			if (d < ACCEPTABLE_ERROR) {
				currentSegment = 0;
			}
		} else if (currentSegment == segments.length - 1) {
			// last segment
			Geopoint t = vertex[vertex.length - 1];
			float d = distTo(t);
			RouteSegment rs = segments[currentSegment];
			tos = new TrackerOverlayState(RouteSegment.ICON_FINISH, 0, getCurrentSegmentInfo(rs),
					"Через " + ((int) d) + " метров", "Конец маршрута");
			if (d < ACCEPTABLE_ERROR) {
				currentSegment++;
			}
			overlay.ShowPoint(null);
		} else if (currentSegment < segments.length) {
			// route is follown
			RouteSegment s = segments[currentSegment];
			RouteSegment ns = segments[currentSegment + 1];
			int ev = ns.segmentStartVertex;
			float d = distTo(vertex[ev]);
			if (d < 100) {
				final String dist = "Через " + ((int) d) + "м";
				final String a = ns.GetAction();
				final String info = getCurrentSegmentInfo(ns);
				tos = new TrackerOverlayState(ns.GetIcon(), getSegmentAngle(ns), dist, a, info);
			} else {
				final String info = getCurrentSegmentInfo(s);
				final String dist = "Осталось " + ((int) d) + "м";
				final String a = ns.GetAction();
				tos = new TrackerOverlayState(ns.GetIcon(), getSegmentAngle(ns), info, dist, a);
			}
			if (d < ACCEPTABLE_ERROR) {
				currentSegment++;
			}
			overlay.ShowPoint(ns.GetAnchor());
		} else {
			// route ended
			tos = new TrackerOverlayState(RouteSegment.ICON_FINISH, 0, "", "Маршрут завершён.", "");
			overlay.ShowPoint(null);
		}

	}

	/**
	 * Call this every frame after {@link #Update()} to draw tracker.
	 */
	public void Draw(Graphics g, int w) {
		Font f = Font.getFont(0, 0, 8);
		int fh = f.getHeight();
		g.setFont(f);
		// bg
		g.setColor(MapOverlay.OVERLAY_BG);
		g.fillRoundRect(5, 5, w - 10, fh * 3 + 10, 10, 10);
		// text
		int x = tos.icon == RouteSegment.NO_ICON ? 10 : (10 + 5 + 50);
		g.setColor(-1);
		g.drawString(tos.line1, x, 10, 0);
		g.drawString(tos.line2, x, 10 + fh, 0);
		g.drawString(tos.line3, x, 10 + fh + fh, 0);
		// icons
		int cx = 10 + 25;
		int cy = 10 + fh + fh / 2;
		if (tos.icon == RouteSegment.MANEUVER_ANGLE) {
			g.setColor(-1);
			final int THICK = 6;
			final int ARROW = 9;
			g.fillRoundRect(cx - THICK / 2, cy - THICK / 2, THICK, 25 + THICK / 2, THICK, THICK);
			float sin = (float) Math.sin(Math.toRadians(tos.angle));
			float cos = (float) Math.cos(Math.toRadians(tos.angle));
			{
				// якоря
				int x25 = (int) (sin * 25);
				int y25 = (int) (cos * 25);
				final float x25t = (sin * (25 - ARROW));
				final float y25t = (cos * (25 - ARROW));

				// оффсеты для линии
				float ldx = (cos * (THICK / 2));
				float ldy = (-sin * (THICK / 2));
				float adx = (cos * ARROW);
				float ady = (-sin * ARROW);
				// стрелка
				int xAl = (int) (cx - x25t - adx);
				int yAl = (int) (cy - y25t - ady);
				int xAr = (int) (cx - x25t + adx);
				int yAr = (int) (cy - y25t + ady);
				// углы линии
				int lfblx = (int) (cx - ldx);
				int lfbly = (int) (cy - ldy);
				int lfbrx = (int) (cx + ldx);
				int lfbry = (int) (cy + ldy);
				int lftlx = (int) (cx - x25t - ldx);
				int lftly = (int) (cy - y25t - ldy);
				int lftrx = (int) (cx - x25t + ldx);
				int lftry = (int) (cy - y25t + ldy);

				g.fillTriangle(lfblx, lfbly, lfbrx, lfbry, lftlx, lftly);
				g.fillTriangle(lftrx, lftry, lfbrx, lfbry, lftlx, lftly);
				g.fillTriangle(cx - x25, cy - y25, xAl, yAl, xAr, yAr);
			}
		} else if (tos.icon != RouteSegment.NO_ICON) {
			g.drawRegion(icons, (tos.icon - 1) * 50, 0, 50, 50, 0, cx, cy, Graphics.VCENTER | Graphics.HCENTER);
		}
	}

	private static String getCurrentSegmentInfo(RouteSegment rs) {
		if (rs instanceof AutoSegment) {
			AutoSegment as = (AutoSegment) rs;
			if (as.street.length() > 0) {
				return as.street + "; " + as.dist + "м";
			}
			return "Дорога " + as.dist + "м";
		}
		return rs.GetDescription();
	}

	private static float getSegmentAngle(RouteSegment rs) {
		if (rs instanceof AutoSegment) {
			AutoSegment as = (AutoSegment) rs;
			return (float) as.angle;
		}
		return 0f;
	}

	private float distTo(Geopoint p) {
		return Distance(extrapolatedGeolocation, p);
	}

	public static float Distance(Geopoint a, Geopoint b) {
		double alat = Math.toRadians(a.lat);
		double alon = Math.toRadians(a.lon);
		double blat = Math.toRadians(b.lat);
		double blon = Math.toRadians(b.lon);
		double cosd = Math.sin(alat) * Math.sin(blat) + Math.cos(alat) * Math.cos(blat) * Math.cos(alon - blon);

		double d = MahoMapsApp.acos(cosd);
		// return d;

		return (float) (d * 6371000D);
	}

}
