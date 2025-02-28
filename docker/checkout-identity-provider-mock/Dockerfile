FROM node:22.13.1-slim

RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends ca-certificates git

WORKDIR /usr/src/app

RUN rm -rf * && \
  git clone https://github.com/pagopa/pagopa-checkout-identity-provider-mock.git /usr/src/app && \
  git checkout main

RUN yarn install && \
    yarn build

CMD ["yarn", "start"]
