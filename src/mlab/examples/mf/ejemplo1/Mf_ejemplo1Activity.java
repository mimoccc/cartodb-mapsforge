package mlab.examples.mf.ejemplo1;

import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Ejemplo básico de mapa on line usando datos de CartoDB
 * @author Matias G. Sticchi
 *
 */
public class Mf_ejemplo1Activity extends MapActivity {

    /////////////////////////////////////////////////////////////
    // Declaracion variables globales
    ////////////////////////////////////////////////////////////

    private MapView mapView;//Vista del mapa
    private ArrayItemizedOverlay itemizedOverlay;//Array para agregar elementos sobre el mapa
    private LocationManager manejadorLocalizacion;// Manejador de geolocalizacion
    private String proveedor;//Proveedor de geolocalizacion (GPS, interner, gsm, etc)
    // punto que marca el centro del mapa.
    private GeoPoint puntoCentral = new GeoPoint(40.68375,-74.00274);





    // Clase para cargar los datos desde CartoDB
    // Se ejecuta de forma asincrona para que no quede bloqueado el dispositivo
    // cuando se estan buscando los datos desde la api de Cartodb.
    private class FetchPointsFromCartoDB extends AsyncTask<Void, Void, org.json.JSONObject> {
        protected org.json.JSONObject doInBackground(Void... params) {

            // Url de acceso a la api con consulta sql
            // API: http://examples.cartodb.com/api/v2/
            // consulta: SELECT st_y(the_geom) as lat, st_x(the_geom) as lng FROM nyc_wifi LIMIT 50
            String url = "http://examples.cartodb.com/api/v2/sql?q=SELECT%20st_y(the_geom)%20as%20lat,%20st_x(the_geom)%20as%20lng%20FROM%20nyc_wifi%20LIMIT%2050";

            // Necesario para realizar el request http
            HttpClient httpclient = new DefaultHttpClient();

            // Se prepara el request con la url
            HttpGet httpget = new HttpGet(url);

            // Ejecutamos el request
            HttpResponse response;
            try {
                response = httpclient.execute(httpget);

                // Necesario para eliminar el status del request
                HttpEntity entity = response.getEntity();

                if (entity != null) {

                    // Necesario para crear el Objeto JSON
                    InputStream instream = entity.getContent();
                    String result= convertStreamToString(instream);

                    // Creacion del Objeto Json
                    JSONObject json= new JSONObject(result);

                    // Cerramos el stream de entrada
                    instream.close();
                    return json;
                }


            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        // Este metodo se ejecuta al finalizar el anterior
        // Metodo para agregar los Markers al mapa a partir del Objeto JSON
        protected void onPostExecute(org.json.JSONObject positions) {

            double lat, lon;

            try{
                // Pasamos a un array los elementos del objeto JSON

                JSONArray valArray=positions.getJSONArray("rows");

                // Iteramos para cada elemento del Array
              for(int i = 0; i < valArray.length(); i++) {

                lat = (Double)(((JSONObject) valArray.get(i)).get("lat"));
                lon = (Double)(((JSONObject) valArray.get(i)).get("lng"));

                // Creamos el punto para representarlo en el mapa
                GeoPoint point = new GeoPoint(lat,lon);

                // Agregamos el punto al array de representacion
                OverlayItem overlayitem = new OverlayItem(point, "", "");
                itemizedOverlay.addItem(overlayitem);
            }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        private String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }

    }

    // Metodo principal para la creacion e la Actividad
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

          ////////////////////////////////////////////////////////////////////////////
        // Con las siguientes lineas geolocalizamos el dispositivo. Descomentar ...
        // para geolocalizar
        ///////////////////////////////////////////////////////////////////////////////

//        manejadorLocalizacion = (LocationManager) getSystemService(LOCATION_SERVICE);
//        Criteria criteria = new Criteria();
//        proveedor = manejadorLocalizacion.getBestProvider(criteria, true);
//        Location localizacion = manejadorLocalizacion.getLastKnownLocation(proveedor);
//        GeoPoint geoPoint = new GeoPoint(localizacion.getLatitude(), localizacion.getLongitude());

//        OverlayItem item = new OverlayItem(geoPoint, "Estoy aquiiiii");
//        itemizedOverlay.addItem(item);

        // Instanciamos el mapa.
        this.mapView = new MapView(this, new MapnikTileDownloader());
        this.mapView.setClickable(true);
        this.mapView.setBuiltInZoomControls(true);

        // centramos el mapa.
        mapView.setCenter(puntoCentral);
        mapView.getController().setZoom(10);

        //Agregamos mapa a la pantalla
        setContentView(mapView);

        //Agregamos los marcadores
        addMarkers();
    }

    public void addMarkers(){
        // Definimos el icono a utilizar para marcar los puntos
        Drawable defaultMarker = getResources().getDrawable(R.drawable.marker);

        // Agregamos la capa de los elementos (al principio vacia)
        itemizedOverlay = new ArrayItemizedOverlay(defaultMarker, true);

        // Insertamos al mapa
        mapView.getOverlays().add(itemizedOverlay);

        // Iniciamos la busqueda de los datos en CartoDB.
        new FetchPointsFromCartoDB().execute();
    }
}