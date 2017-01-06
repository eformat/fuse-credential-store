Fuse Vault
==========

Provides a facility to include passwords and other sensitive strings as masked strings that are resolved from an
encrypted vault.

The built-in support is for OSGI environment, specifically for Apache Karaf, and for Java system properties.

You might have specified passwords, for instance `javax.net.ssl.keyStorePassword`, as system properties in clear
text this project allows you to specify these values as references to an encrypted vault.

With the Fuse Vault installed you can specify those sensitive strings as references to a value stored in encrypted
vault, so the clear text value now becomes `VAULT::block1::key::1` referencing the `block1` of the vault and the
value stored under the `key` attribute name there, the `1` at the end is the shared key not currently used in the 
underlying implementation.

Getting started
---------------

First you need to build and install the `vault-karaf-core` to your local Maven repository by runing:

    $ ./mvnw

from this directory.

Next, if you do are adding the vault support to a new Karaf download and extract
[Apache Karaf distribution](http://karaf.apache.org/download.html).

    $ curl -O http://www.apache.org/dyn/closer.lua/karaf/4.0.8/apache-karaf-4.0.8.tar.gz
    $ tar xf apache-karaf-4.0.8.tar.gz

Change into `apache-karaf-4.0.8` directory and run the `bin/karaf` to startup the container, and then install the
Fuse Vault bundle.

    $ cd apache-karaf-4.0.8
    $ bin/karaf
            __ __                  ____      
           / //_/____ __________ _/ __/      
          / ,<  / __ `/ ___/ __ `/ /_        
         / /| |/ /_/ / /  / /_/ / __/        
        /_/ |_|\__,_/_/   \__,_/_/         
    
      Apache Karaf (4.0.8)
    
    Hit '<tab>' for a list of available commands
    and '[cmd] --help' for help on a specific command.
    Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.
    
    karaf@root()> bundle:install -s mvn:org.jboss.fuse.vault/vault-karaf-core/0.0.1-SNAPSHOT

Next create a vault using `vault:create`:

    karaf@root()> vault:create -s Mxyzptlk -p 'my very secret password' -v my-vault
    New vault was created in: .../apache-karaf-4.0.8/my-vault
    To use it specify the following environment variables:
    export KEYSTORE_URL=.../apache-karaf-4.0.8/my-vault/vault.keystore
    export SALT=Mxyzptlk
    export ITERATION_COUNT=100
    export KEYSTORE_PASSWORD=MASK-MfBSNdHBps/o9PcViobYMdruEtF1nlMJ
    export KEYSTORE_ALIAS=vaultkey
    export ENC_FILE_DIR=.../apache-karaf-4.0.8/my-vault

This should have created `my-vault` directory with two files in it JCEKS KeyStore containing the secret key, and the 
encrypted vault file holding all the secret strings.

    $ tree my-vault/
    my-vault/
    ├── VAULT.dat
    └── vault.keystore
    
    0 directories, 2 files

Next add your secrets to the vault by using `vault:store`:

    karaf@root()> vault:store -b block1 -a secret -x 'my very worst secret'
    Value stored in vault to reference it use: VAULT::block1::secret::1

Exit the Karaf container by issuing `logout`:

    karaf@root()> logout

Set the required environment variables presented when creating the vault, and run the Karaf again specifying the
reference to your secret instead of the value:

    $ export KEYSTORE_URL=.../apache-karaf-4.0.8/my-vault/vault.keystore
    $ export SALT=Mxyzptlk
    $ export ITERATION_COUNT=100
    $ export KEYSTORE_PASSWORD=MASK-MfBSNdHBps/o9PcViobYMdruEtF1nlMJ
    $ export KEYSTORE_ALIAS=vaultkey
    $ export ENC_FILE_DIR=.../apache-karaf-4.0.8/my-vault
    $ EXTRA_JAVA_OPTS="-Djavax.net.ssl.keyStorePassword=VAULT::block1::secret::1" bin/karaf

And the value of `javax.net.ssl.keyStorePassword` when accessed using `System::getProperty` should contain the
string `"my very worst secret"`.

Security
--------

This is password masking, so if the environment variables are leaked outside of your environment or intended use along
with the content of the vault directory, your secretes are compromised. The value of the property when accessed through
JMX gets replaced with the string `"<sensitive>"`, but do note that there are many code paths that lead to 
`System::getProperty`, for instance diagnostics or monitoring tools might access it along with any 3rd party software
for debugging purposes.
