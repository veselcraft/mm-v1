package mahomaps.route;

import org.json.me.JSONObject;

import mahomaps.map.Geopoint;

public class Route {

	public Geopoint[] points;
	public RouteSegment[] segments;
	public String time;
	public String distance = "Неизвестно";

	public Route(JSONObject route) {
		JSONObject props = route.getJSONObject("properties");
		JSONObject meta = props.getJSONObject("PathMetaData");
		time = meta.getJSONObject("Duration").getString("text");
		JSONObject dist = meta.optJSONObject("Distance");
		if (dist == null)
			dist = meta.getJSONObject("WalkingDistance");
		if (dist != null)
			distance = dist.getString("text");
		points = RouteDecoder.DecodeRoutePath(props.getString("encodedCoordinates"));
		segments = RouteDecoder.DecodeSegments(route.getJSONArray("features"), points);
	}

}
