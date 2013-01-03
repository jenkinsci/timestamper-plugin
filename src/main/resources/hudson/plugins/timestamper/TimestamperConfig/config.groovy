package hudson.plugins.timestamper.TimestamperConfig

def f=namespace(lib.FormTagLib)

f.section(title:_("Timestamper")) {
    f.entry(title: _("System clock time format"), field:"timestampFormat") {
        f.textbox()
    }
    f.entry(title: _("Elapsed time format"), field:"elapsedTimeFormat") {
        f.textbox()
    }
}