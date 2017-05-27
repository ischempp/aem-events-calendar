var SlingSettingsService = Packages.org.apache.sling.settings.SlingSettingsService;
 
use(function () {
    // Get runmodes and transform them into an object that is easier to read for Sightly
    var runmodesObj = {};
    var runmodesSet = sling.getService(SlingSettingsService).getRunModes();
    var iterator = runmodesSet.iterator();
    
    while (iterator.hasNext()) {
        runmodesObj[iterator.next()] = true;
    }
    
    return {
        runmodes: runmodesObj
    }
});
