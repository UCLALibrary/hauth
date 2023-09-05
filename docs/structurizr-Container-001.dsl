workspace {
    model {
        dlp_staff = person "DLP Staff" "A staff member in the Digital Library Program"
        user = person "User" "An end user of the UCLA Library Digital Collections or Sinai Manuscripts Digital Library"

        ucla_dl_image_delivery = softwareSystem "UCLA Digital Library image delivery" {
            iiif_viewer = container "IIIF Viewer" "A user interface that renders a IIIF image viewer" "UV or Mirador" {
                tags "User Interface"
                user -> this "interacts with"
            }
            fester = container Fester "IIIF manifest store" "UCLALibrary/fester" {
                iiif_viewer -> this "sends HTTP requests for IIIF Manifests to"
            }
            hauth = container Hauth "IIIF Authentication API 1.0 implementation" "UCLALibrary/hauth" {
                iiif_viewer -> this "sends HTTP requests for access cookies and access tokens to"
                dlp_staff -> this "administers image access mode records with a script that sends HTTP requests to"
            }
            cantaloupe = group "Cantaloupe instance" {
                server_app = container "Cantaloupe Image Server" "IIIF image server" "cantaloupe-project/cantaloupe"
                delegate = container "Cantaloupe Auth Delegate" "External JAR with a main class that implements the Cantaloupe delegate interface" "UCLALibrary/cantaloupe-auth-delegate"

                iiif_viewer -> server_app "sends HTTP requests for image information (info.json) and image pixels to"
                server_app -> delegate "fetches from build-artifacts S3 bucket and loads with classloader on startup, and subsequently calls on every image and info.json request"
                delegate -> hauth "sends HTTP requests for an image's access mode to"
            }
            postgres = container "PostgreSQL" {
                tags "Database"
                hauth -> this "stores/retrieves an image's access mode in/from"
            }
        }
    }
    views {
        container ucla_dl_image_delivery {
            include *
        }
        styles {
            element "User Interface" {
                shape WebBrowser
            }
            element "Database" {
                shape Cylinder
            }
        }
        theme default
    }
}
