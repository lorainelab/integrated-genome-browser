APPS = demo tutorial

.PHONY: all bioviz $(APPS) clean docs apps-clean bioviz-clean

all: $(APPS)

clean: bioviz-clean apps-clean

bioviz:
	cd src && $(MAKE);

bioviz-clean:
	cd src && $(MAKE) clean;

docs:
	cd src && $(MAKE) docs;

$(APPS): bioviz
	cd $@ && $(MAKE);

apps-clean: $(addsuffix -clean, $(APPS))
	@echo cleaned $(APPS)

%-clean:
	cd $* && $(MAKE) clean;
