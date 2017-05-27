"use strict";

/**
 * Event details component JS backing script
 */
use(['/libs/wcm/foundation/components/utils/AuthoringUtils.js',
'/libs/wcm/foundation/components/utils/Image.js',
'/libs/sightly/js/3rd-party/q.js'], function ( AuthoringUtils, Image, Q ) {

  var CONST = {
    MAP_LINK_TEXT: "[View Map]",
    SECTION_CSS_CLASS: "eventdetails-section",
    WRAPPER_CSS_CLASS: "fh-component-eventdetails",
    IMG_CONTAINER_CSS_CLASS: "eventdetails-image fh-component-image",
    AUTHOR_IMAGE_CLASS: 'cq-dd-image',
    PLACEHOLDER_SRC: '/etc/designs/default/0.gif',
    PLACEHOLDER_TOUCH_CLASS: 'cq-placeholder file',
    PLACEHOLDER_CLASSIC_CLASS: 'cq-image-placeholder'
  };

  var details = {},
      imageDefer = Q.defer();
      imagePath = currentPage.getPath() + '/jcr:content/image';

  // Adding the constants to the exposed API
  details.CONST = CONST;
  
  granite.resource.resolve(imagePath).then(function(res) {  
    if (res.properties["fileReference"]) {
      var image = new Image(res); 
      imageDefer.resolve(image);
    } else {
      granite.resource.resolve(imagePath + "/file").then(function (localImage) {
        var image = new Image(localImage);
        imageDefer.resolve(image);
      });
    }
  });
  
  details.image = imageDefer.promise;
  
  return details;
});
