# esignet-plugins
Repository hosting source code for esignet and esignet-signup java runtime dependencies plugins.

## eSignet/esignet-signup service image bundled with plugins
We release esignet service and esignet-signup service images with the latest published plugin jars  
available at the time of the esignet and esignet-signup service release.

**Note**: We publish two flavors of esignet docker images:
1. esignet docker image—This is the base esignet image.
2. esignet-with-plugins docker image—This image is built using esignet base image bundled with all default plugins available in this repository. 

Any bug fixes or changes in the plugins will be made available in the esignet service or
esignet-signup service in their immediate next releases.

### How to use the plugin with fixes where the eSignet is not yet released?
One can use the esignet base image to test the new fixes in the plugin. There are two ways:

1. Pass URL to download the plugin zip/jar in the "plugin_url_env" environment variable of the container.
2. Mount the external directory with the plugin onto "/home/mosip/plugins" directory in the container.

Either of the above 2 steps should be followed and finally set the "plugin_name_env" environment variable. With this setup, eSignet
service should get started with the configured plugin.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
