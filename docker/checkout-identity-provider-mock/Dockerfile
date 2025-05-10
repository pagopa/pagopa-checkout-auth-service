FROM node:22.13.1-slim@sha256:83fdfa2a4de32d7f8d79829ea259bd6a4821f8b2d123204ac467fbe3966450fc

RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends ca-certificates git

WORKDIR /usr/src/app

RUN rm -rf * && \
  git clone https://github.com/pagopa/pagopa-checkout-identity-provider-mock.git /usr/src/app && \
  git checkout main

RUN yarn install && \
    yarn build

CMD ["yarn", "start"]
