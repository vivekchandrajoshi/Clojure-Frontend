var htAppConfig = {
  appId: "cpe",
  buildId: "dev",

  // needed when hosted outside of portal as is the case
  // when hosting in figwheel. use the appropriate port number
  // on which the portal service is listening.
  // var portalSiteRoot = "http://" + location.hostname + ":3000";
  portalUri: "https://my-dev.topsoe.com",

  serviceUri: "https://topsoe-dev-cpe.azurewebsites.net",
//  serviceUri: "http://localhost:8000",

  languages: [
    { code: 'en', flag: 'gb', name: 'English' },
    { code: 'es', flag: 'es', name: 'Español' },
    { code: 'ru', flag: 'ru', name: 'русский' },
  ],
}
