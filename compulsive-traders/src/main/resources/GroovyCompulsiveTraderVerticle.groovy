import io.vertx.groovy.core.CompositeFuture
import io.vertx.groovy.core.Future
import io.vertx.groovy.core.eventbus.MessageConsumer
import io.vertx.groovy.servicediscovery.types.EventBusService;
import io.vertx.groovy.servicediscovery.types.MessageSource;
import io.vertx.groovy.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.ServiceDiscovery
import io.vertx.servicediscovery.types.MessageSource
import io.vertx.workshop.portfolio.PortfolioService
import io.vertx.workshop.trader.impl.TraderUtils

def company = TraderUtils.pickACompany();
def numberOfShares = TraderUtils.pickANumber();

println("Groovy compulsive trader configured for company " + company + " and shares: " + numberOfShares);

// We create the discovery service object.
def discovery = ServiceDiscovery.create(vertx);

io.vertx.core.Future<io.vertx.core.eventbus.MessageConsumer<Map>> marketFuture = io.vertx.core.Future.future();
io.vertx.core.Future<PortfolioService> portfolioFuture = io.vertx.core.Future.future();

MessageSource.getConsumer(discovery,
        ["name" : "market-data"], marketFuture.completer());
EventBusService.getProxy(discovery,
        "io.vertx.workshop.portfolio.PortfolioService", portfolioFuture.completer());

// When done (both services retrieved), execute the handler
io.vertx.core.CompositeFuture.all(marketFuture, portfolioFuture).setHandler( { ar ->
  if (ar.failed()) {
    System.err.println("One of the required service cannot be retrieved: " + ar.cause());
  } else {
    // Our services:
    PortfolioService portfolio = portfolioFuture.result();
    io.vertx.core.eventbus.MessageConsumer<Map> marketConsumer = marketFuture.result();

    // Listen the market...
    marketConsumer.handler( { message ->
      Map quote = message.body();
      TraderUtils.dumbTradingLogic(company, numberOfShares, portfolio, quote);
    });
  }
});