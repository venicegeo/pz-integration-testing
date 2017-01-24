"use strict";

var Map = function(parent) {
    
    // parent is passed in because there are two maps that are very, very similar.
    // one map is for exploring and the other for analyzing

    var me = parent.$('openlayermap');

    // arguments[0] is passed to the executeScript function. it is dom object
    function centerMap(lat, long){
       return "map = angular.element(arguments[0]).scope().settings.mapReference; \
       map.getView().setCenter(ol.proj.transform([" + long + "," + lat +"], 'EPSG:4326', 'EPSG:3857')); \
       map.getView().setZoom(6);"
   }

   this.selectCountries = function(countries){
       for(let country of countries){
           this.selectCountry(country);
        }
   }

   this.selectCountry = function(country){
       var countries = {
           'UNITED STATES': {lat:40.4 , long:-101.3},
           'CANADA': {lat:56.1 , long:-107.3},
           'MEXICO': {lat: 23.6, long:-102.6},
           'CHINA': {lat: 34.4 , long: 103.6},
           'INDIA': {lat: 22.6, long: 79.4},
           'AUSTRALIA': {lat: -25.1, long: 134.3},
        }

        var center = countries[country.toUpperCase()];

        return browser.executeScript(centerMap(center.lat, center.long), me).then(()=>{
            me.click();
        })
     }

  };

  module.exports = Map