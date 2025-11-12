# narutbae - 나룻배

narutbae is a lazy Arch Linux mirror that will download packages on demand.

# Usage

## Structure & HTTP server configuration

narutbae needs to work in pair with a HTTP server such as nginx.

narutbae will manage a expose directory, where (symlinks to) files that were previously downloaded are placed neatly.
Actual downloads will happen from this directory, but we need to check if we actually have the requested file first. 
Therefore, all requests are redirected to narutbae first (sample nginx config):

```
        location /[NARUTBAE_REPO_NAME]/ {
                proxy_pass http://narutbae;
                proxy_redirect off;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Host $server_name;
        }
```

narutbae will check if it has the file and make a HTTP 301 to `/downloads/[FILENAME]` if it has it, so we cover that path with the HTTP server:

```
        location ^~ /[NARUTBAE_REPO_NAME]/downloads {
                alias [NARUTBAE_EXPOSE_AT];
        }
```

However, we still want the index page to be rendered properly so users can check the files we have, so we handle it specially:

```
        location = /[NARUTBAE_REPO_NAME]/ {
                root [NARUTBAE_EXPOSE_AT];
                autoindex on;
        }
```

Joined in the proper order, it looks like this:
```
        location = /[NARUTBAE_REPO_NAME]/ {
                root [NARUTBAE_EXPOSE_AT];
                autoindex on;
        }

        location ^~ /[NARUTBAE_REPO_NAME]/downloads {
                alias [NARUTBAE_EXPOSE_AT];
        }
        
        location /[NARUTBAE_REPO_NAME]/ {
                proxy_pass http://narutbae;
                proxy_redirect off;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Host $server_name;
        }
```

narutbae also uses a store directory to actually store files (expose directory is made up of symlinks that point to the store directory).
The store directory is required to have this structure at initial state:

```
[STORE DIRECTORY ROOT]
└── .narutbae
    └── symlinkbase  
        └── (empty directory)
```

narutbae will automatically remove files that haven't been accessed for a configured amount of time by keeping track of requests.


## Required parameters

The following environment variables have to be set:
```shell
NARUTBAE_GC_DAYS=7 # obviously, in days
NARUTBAE_GC_INTERVAL=1440 # in minutes
NARUTBAE_SYNC_INTERVAL=10 # in minutes
NARUTBAE_EXPOSE_AT= # expose directory path
NARUTBAE_STORE_AT= # store directory path
NARUTBAE_REPO_NAME=
NARUTBAE_TARGET="https://"
```

The `.db` file to sync against is expected to be at `${NARUTBAE_TARGET}${NARUTBAE_REPO_NAME}.db`.